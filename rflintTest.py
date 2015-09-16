import unittest
from rflint import RFLint, KeywordRule, UnexpectedKeywordException

###
# analyze-map_func()
# DSL
# reader - subclasses
# generate tokens
# extensions/processor/mixin
# def _state_global(self, token):
#         if token in ("class", "namespace"):
#             self._state = self._state_namespace_def

#   for token in tokens:
#         reader.state(token)
#         yield token
#     reader.eof()
    
#   def state(self, token):
#         self._state(token)
# OutputScheme
# whitelist_filter   any()
# rule<... condition_A, condition_B


class RFLintTest(unittest.TestCase):
    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_count_lines(self):
        lint = RFLint('sample.txt')
        lint.rules([])
        
        lint.check()

        self.assertEqual(9, lint.stat['line_count'])
        self.assertEqual(2, lint.stat['comment_line_count'])
        self.assertEqual(1, lint.stat['empty_line_count'])
        self.assertEqual(2, lint.stat['section_count'])
        self.assertEqual(4, lint.stat['row_count'])
        # print lint.terule_liststcases

    def test_rules_should_not_contain_keywords_group(self):
        lint = RFLint('sample.txt')
        lint.rules([KeywordRule(should_not_contain=['Sleep'], will_result_in=UnexpectedKeywordException)])
        # lint.rule('AnyKeyword', NotWithInLayer='ElementInteraction', ShouldNotUse='Selenium2LibKeywords', WillResultIn='ERROR')


        with self.assertRaises(UnexpectedKeywordException) as cm:
            lint.check()
        the_exception = cm.exception
        self.assertEqual(the_exception.error_msg, 'Sleep')

if __name__ == "__main__":
    #import sys;sys.argv = ['', 'Test.testName']
    unittest.main()
