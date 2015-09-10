object rfCheckStyle {
  def main(args: Array[String]) {
    //TODO: Check args format and provide help
    println("Begin to find bugs for RF Script in:" + args(0) + "...")
    val lstViolation = rfChecker.check(rfParser.parse(args(0)), myRules)
    println("Total " + lstViolation.length + " Violation Found")
    lstViolation.foreach(println)
    println("job done!")
  }
}

object myRules extends rfRules {
  //allowed Dependency, you can add you own rules here
  // you can also create new script type or override existing type
  val allowedDependency = Set(
    /* -------------Modify here if need------------ */
    TestCase CanImport BusinessFlow,
    BusinessFlow CanImport PageComponent,
    PageComponent CanImport ElementInteraction 
    /* -------------Modify here if need------------ */ )

  val allowedLib = Set(
    /* -------------Modify here if need------------ */
    ElementInteraction CanImport Selenium2Library,
    ElementInteraction CanImport AutoItLibrary,
    ElementInteraction CanImport QTLibrary 
    /* -------------Modify here if need------------ */ )

  val lstKeywordRules = Set(
    AnyKeyword NotWithInLayer ElementInteraction ShouldNotUse Selenium2LibKeywords WillResultIn ERROR Violation
      ("Keyword Layer Violation", "Selenium2Lib Keywords should not be used except within ElementInteraction Layer"),
    Sleep WithInLayer ElementInteraction ShouldNotUsed () WillResultIn WARNING Violation
      ("Sleep Violation", "Keyword Sleep should not be used within ElementInteraction Layer"))
}

abstract class rfRules extends rfRuleCommon with rfDependencyRule with rfLibImportRule with rfKeywordRule {
}

trait rfRuleCommon {
  //Violation severity level
  val ERROR = "Error"
  val WARNING = "Warning"
  val SUGGESSTION = "Suggestion"

  class rfNoScriptTypeViolation(s: String) extends rfViolation(ERROR, "Resource Dependency Violation", s) {
  }
}

trait rfDependencyRule extends rfRuleCommon {
  //Predefined script Type used below in allowed Dependency, you can override or create your own
  val TestCase = "Test Case"
  val BusinessFlow = "Business Flow"
  val PageComponent = "Page Component"
  val ElementInteraction = "Element Interaction"
  val allLayers = List(TestCase, BusinessFlow, PageComponent, ElementInteraction)

  val allowedDependency: Set[(String, String)]

  class rfDependencyViolation(s: String) extends rfViolation(ERROR, "Resource Dependency Violation", s) {
  }

  // Allow Rule Define DSLs
  class ScriptType(source: String) {
    def CanImport(dest: String): Tuple2[String, String] = Tuple2(source, dest)
  }

  implicit def stringToScriptType(x: String): ScriptType = new ScriptType(x)
}

trait rfKeywordRule extends rfDependencyRule with Selenium2LibKeywords {
  val lstKeywordRules: Set[KeywordRule]
  val AnyKeyword = "Any"
  val Sleep = "Sleep"

  class KeywordRule(k: String, l: List[String], f: String => Boolean, el: String, t: String, m: String) {
    val keyword = k
    val effectiveLayers: List[String] = l
    val condition = f
    val errorLevel = el
    val vType = t
    val message = m

    //Auxiliary constructor
    def this(k: String) = this(k, Nil, _.isEmpty(), "", "", "")
    def this(k: String, l: List[String]) = this(k, l, _.isEmpty(), "", "", "")
    def this(k: String, l: List[String], f: String => Boolean) = this(k, l, f, "", "", "")
    def this(k: String, l: List[String], f: String => Boolean, el: String) = this(k, l, f, el, "", "")

    def WithInLayer(layer: String) = new KeywordRule(keyword, layer :: Nil)
    def NotWithInLayer(layer: String) = new KeywordRule(keyword, allLayers.filterNot(_.equals(layer)))
    def ShouldNotUse(l: List[String]) = new KeywordRule(keyword, effectiveLayers, x => (!l.exists(x.equalsIgnoreCase(_))))
    def ShouldNotUsed() = new KeywordRule(keyword, effectiveLayers, x => false)
    def WillResultIn(level: String) = new KeywordRule(keyword, effectiveLayers, condition, level)
    def Violation(t: String, m: String) = new KeywordRule(keyword, effectiveLayers, condition, errorLevel, t, m)

    override def toString = "Keyword:" + keyword + " Effective Layers:" + effectiveLayers.toString()
  }
  implicit def stringToKeywordRule(x: String): KeywordRule = new KeywordRule(x)
}

trait rfLibImportRule extends rfRuleCommon {
  //Predefined Library Name
  val Selenium2Library = "Selenium2"
  val AutoItLibrary = "AutoIt"
  val QTLibrary = "QT"

  val allowedLib: Set[(String, String)]

  class rfLibImportViolation(s: String) extends rfViolation(ERROR, "Library Import Violation", s) {
  }
}

class rfScript(p: String, t: String, l: List[String], r: List[String], k: List[rfKeyword]) {
  val path = p;
  val scriptType = t
  val libs = l
  val resources = r
  val keywords = k
}

class rfKeyword(n: String, s: List[Tuple2[String, String]]) {
  val name = n;
  val lstStep = s;
}

class rfViolation(l: String, t: String, s: String) {
  val level: String = l
  val vType: String = t
  val message: String = s

  override def toString = level + ":" + vType + ":" + message
}

import java.io.File

object rfParser {
  def parse(path: String): List[rfScript] =
    parseFileList(getFileTree(new File(path)).filter(_.getName.endsWith(".txt")))

  private def getFileTree(f: File): Stream[File] =
      f #:: (if (f.isDirectory) f.listFiles().toStream.flatMap(getFileTree) else Stream.empty)

  private def parseFileList(lstFile: Stream[File]): List[rfScript] = 
    lstFile.toList.map(parseRFScript(_))

  private def parseRFScript(script: File): rfScript = {
    import scala.io.Source
    val lstLines = Source.fromFile(script)("UTF-8").getLines()

    //Empty File
    if (!lstLines.hasNext)
      return new rfScript(script.getPath(), "", Nil, Nil, Nil)

    if (!lstLines.next.contains("Settings"))
      return new rfScript(script.getPath(), "", Nil, Nil, Nil)

    try {
      val scriptType = getScriptType(lstLines.next)

      val lstReference = getAllReferences(lstLines)

      var lstKeyword: List[rfKeyword] = Nil
      if (lstReference._3.contains("Keywords") || jumpToKeywordsSection(lstLines))
        lstKeyword = getAllKeywords(lstLines)

      new rfScript(script.getPath(), scriptType, lstReference._2, lstReference._1, lstKeyword)
    } catch {
      case ex: RuntimeException => return new rfScript(script.getPath(), "", Nil, Nil, Nil)
    }
  }
  
  //TODO: Rewrite using Pattern Matching
  private def getScriptType(line: String): java.lang.String = {
    val scriptAttrMarker = "---"
    //Valid RF script should have a documentation section that contains script attribute type  
    if (!line.contains("Documentation") || !line.contains(scriptAttrMarker))
      throw new RuntimeException("Valid RF script should have a documentation section that contains script attribute type")

    val firstMarkerIndex = line.indexOf(scriptAttrMarker)
    val secondMarkerIndex = line.lastIndexOf(scriptAttrMarker)

    if (secondMarkerIndex == firstMarkerIndex)
      throw new RuntimeException("Script attribute type should be surrounded by " + scriptAttrMarker)

    line.substring(firstMarkerIndex + 3, secondMarkerIndex)
  }

  //TODO: Rewrite using pattern matching
  //TODO: Rewrite using closure
  private def getAllReferences(lstLines: Iterator[String]): Tuple3[List[String], List[String], String] = {
    val list = if (lstLines.hasNext) lstLines.next() else ""

    if (list.eq("") || list.contains("***"))
      new Tuple3(Nil, Nil, list)
    else if (list.contains("Resource")) {
      val resource = list.substring(list.indexOf("Resource") + 8, list.length).trim()
      val rest = getAllReferences(lstLines)
      new Tuple3(resource :: rest._1, rest._2, rest._3)
    } else if (list.contains("Library")) {
      val library = list.substring(list.indexOf("Library") + 7, list.length).trim()
      val rest = getAllReferences(lstLines)
      new Tuple3(rest._1, library :: rest._2, rest._3)
    } else
      getAllReferences(lstLines)
  }

  private def jumpToKeywordsSection(lstLines: Iterator[String]): Boolean = 
    if(!lstLines.hasNext) false 
    else if (lstLines.next().contains("Keywords")) true 
    else jumpToKeywordsSection(lstLines)

  //TODO: Consider using flatMap
  private def getAllKeywords(lstLines: Iterator[String]): List[rfKeyword] =
    if (lstLines.hasNext) new rfKeyword(lstLines.next().trim(), getAllSteps(lstLines))::getAllKeywords(lstLines) else Nil

  //TODO: Rewrite using pattern matching
  private def getAllSteps(lstLines: Iterator[String]): List[Tuple2[String, String]] = {
    val step = if (lstLines.hasNext) lstLines.next().trim() else ""
    if (step == "") {
      return Nil
    } else {
      if (step.contains("  "))
        new Tuple2(step.split("  ")(0), step.split("  ")(1)) :: getAllSteps(lstLines)
      else
        new Tuple2(step, "") :: getAllSteps(lstLines)
    }
  }
}

//TODO: How to support rule extension
object rfChecker {
  def check(lstScript: List[rfScript], rules: rfRules): List[rfViolation] =
    lstScript.flatMap(checkFile(_,rules,lstScript))   
  private def checkFile(s: rfScript, rules: rfRules, context: List[rfScript]): List[rfViolation] = {
    def checkLib(lib: String): List[rfViolation] =
      if (!rules.allowedLib.exists(x => x._1.equals(s.scriptType) && lib.contains(x._2)))
        new rules.rfLibImportViolation("script " + s.path + " of type " + s.scriptType + " is not allowed to depend on lib:" + lib) :: Nil
      else
        Nil
    def checkResource(resource: String): List[rfViolation] = {
      val resScriptType = getResourceScriptType(resource, context) 
      if (!rules.allowedDependency.exists(x => x._1.equals(s.scriptType) && x._2.equals(resScriptType)))
        new rules.rfDependencyViolation("script " + s.path + " of type " + s.scriptType + " is not allowed to depend on " + resource + " of type " + resScriptType) :: Nil
      else
        Nil
     }  
    def checkKeyword(keyword: rfKeyword): List[rfViolation] =
      keyword.lstStep.flatMap(checkStep(_))
    def checkStep(keyword: Tuple2[String, String]): List[rfViolation] = {
      val lstFilteredRule = rules.lstKeywordRules.filter(x =>
        (x.keyword.equals(rules.AnyKeyword) || x.keyword.equals(keyword._1)) &&
          (x.effectiveLayers.exists(_.equalsIgnoreCase(s.scriptType))))

      for (rule <- lstFilteredRule)
        if (!rule.condition(keyword._1))
          return (new rfViolation(rule.errorLevel, rule.vType, keyword._1 + " is used in script:" + s.path + " of type " + s.scriptType) :: Nil)
      Nil
    }

    if (s.scriptType == "")
      new rules.rfNoScriptTypeViolation("script " + s.path + " has no attribute tag!") :: Nil
    else
      s.libs.flatMap(checkLib(_)) ::: s.resources.flatMap(checkResource(_)) ::: s.keywords.flatMap(checkKeyword(_))
  }

  private def getResourceScriptType(resPath: String, lstScriptFile: List[rfScript]): String = {
    for (script <- lstScriptFile) {
      //TODO:The assumption made here is that the resPath contains just file name and resPath is unique named
      if (script.path.trim().toLowerCase().contains(resPath.trim().toLowerCase()))
        return script.scriptType
    }
    return "NOT FOUND"
  }
}

trait Selenium2LibKeywords {
  val Selenium2LibKeywords = List(
    "Alert Should Be Present",
    "Assign Id To Element",
    "Capture Page Screenshot",
    "Checkbox Should Be Selected",
    "Checkbox Should Not Be Selected",
    "Choose Cancel On Next Confirmation",
    "Choose File",
    "Choose Ok On Next Confirmation",
    "Click Button",
    "Click Element",
    "Click Image",
    "Click Link",
    "Close All Browsers",
    "Close Browser",
    "Close Window",
    "Confirm Action",
    "Current Frame Contains",
    "Delete All Cookies", "Delete Cookie",
    "Double Click Element",
    "Element Should Be Disabled",
    "Element Should Be Enabled",
    "Element Should Be Visible",
    "Element Should Contain",
    "Element Should Not Be Visible",
    "Element Text Should Be",
    "Execute Async Javascript",
    "Execute Javascript",
    "Focus",
    "Frame Should Contain",
    "Get Alert Message",
    "Get All Links",
    "Get Cookie Value",
    "Get Cookies",
    "Get Element Attribute",
    "Get Horizontal Position",
    "Get List Items",
    "Get Location",
    "Get Matching Xpath Count",
    "Get Selected List Label",
    "Get Selected List Labels",
    "Get Selected List Value",
    "Get Selected List Values",
    "Get Selenium Implicit Wait",
    "Get Selenium Speed",
    "Get Selenium Timeout",
    "Get Source",
    "Get Table Cell",
    "Get Title",
    "Get Value",
    "Get Vertical Position",
    "Get Window Identifiers",
    "Get Window Names",
    "Get Window Titles",
    "Go Back",
    "Go To",
    "Input Password",
    "Input Text",
    "List Selection Should Be",
    "List Should Have No Selections",
    "Location Should Be",
    "Location Should Contain",
    "Log Location",
    "Log Source",
    "Log Title",
    "Maximize Browser Window",
    "Mouse Down",
    "Mouse Down On Image",
    "Mouse Down On Link",
    "Mouse Out",
    "Mouse Over",
    "Mouse Up",
    "Open Browser",
    "Open Context Menu",
    "Page Should Contain",
    "Page Should Contain Button",
    "Page Should Contain Checkbox",
    "Page Should Contain Element",
    "Page Should Contain Image",
    "Page Should Contain Link",
    "Page Should Contain List",
    "Page Should Contain Radio Button",
    "Page Should Contain Textfield",
    "Page Should Not Contain",
    "Page Should Not Contain Button",
    "Page Should Not Contain Checkbox",
    "Page Should Not Contain Element",
    "Page Should Not Contain Image",
    "Page Should Not Contain Link",
    "Page Should Not Contain List",
    "Page Should Not Contain Radio Button",
    "Page Should Not Contain Textfield",
    "Press Key",
    "Radio Button Should Be Set To",
    "Radio Button Should Not Be Selected",
    "Register Keyword To Run On Failure",
    "Reload Page",
    "Select All From List",
    "Select Checkbox", "Select Frame",
    "Select From List",
    "Select Radio Button",
    "Select Window",
    "Set Browser Implicit Wait",
    "Set Selenium Implicit Wait",
    "Set Selenium Speed",
    "Set Selenium Timeout",
    "Simulate",
    "Submit Form",
    "Switch Browser",
    "Table Cell Should Contain",
    "Table Column Should Contain",
    "Table Footer Should Contain",
    "Table Header Should Contain",
    "Table Row Should Contain",
    "Table Should Contain",
    "Textfield Should Contain",
    "Textfield Value Should Be",
    "Title Should Be",
    "Unselect Checkbox",
    "Unselect Frame",
    "Unselect From List",
    "Wait For Condition",
    "Wait Until Page Contains",
    "Wait Until Page Contains Element",
    "Xpath Should Match X Times")
}