import re
from pipe import *

class KeywordRule(object):
    """docstring for KeywordRule"""
    def __init__(self, should_not_contain):
        super(KeywordRule, self).__init__()
        self.should_not_contain = should_not_contain | select(lambda x: x.lower()) | as_list
        print self.should_not_contain

    def validate(self, testcases):
        print '==', testcases
        for casename, steps in testcases.iteritems():
            for step in steps:
                keyword = step[1] if step[0].startswith('$') else step[0]
                if keyword.lower() in self.should_not_contain:
                    raise UnexpectedKeywordException(casename + ' contains: Sleep')

        
class UnexpectedKeywordException(Exception):
    """docstring for UnexpectedKeywordException"""
    def __init__(self, keyword_name):
        super(UnexpectedKeywordException, self).__init__()
        self.keyword_name = keyword_name

    @property
    def error_msg(self):
        return self.keyword_name
    
        
class RFLint(object):
    """docstring for RFLint"""
    
    section_prefixs = ['S', 'T', 'V', 'K']

    def __init__(self, filename):
        super(RFLint, self).__init__()
        self.filename = filename
        self.stat = {'line_count': 0, 'empty_line_count': 0, 'comment_line_count':0, 'row_count':0, 'section_count': 0}
        self.testcases = {}
        self.curr_testcase = None
        self.rules = []

    # def rule(self, element, NotWithInLayer=None, ShouldNotUse=None, WillResultIn=None):
    #     pass

    def rule(self, a_rule):
        self.rules.append(a_rule)
        return self


    def check(self):
        with open(self.filename) as f:
            for raw_line in f:
                self.stat['line_count'] += 1
                line = raw_line.lstrip().lower()
                if line == '':
                    self.stat['empty_line_count'] += 1
                elif line.startswith(('#', 'comment')):
                    self.stat['comment_line_count'] += 1
                elif line.startswith('***'):
                    self.section_type = line[3:].lstrip()[0]
                    self.on_section(line, self.section_type)
                else:
                    self.stat['row_count'] += 1
                    self.on_row(line)

        for rule in self.rules:
            print rule.should_not_contain
            rule.validate(self.testcases)

        
        


    def on_section(self, line, section_type):
        # print line, section_type
        self.stat['section_count'] += 1

    def on_row(self, row):
        _space_splitter = re.compile(' {2,}|\t|\n')
        column = _space_splitter.split(row)
        if self.curr_testcase is None:
            self.curr_testcase = column[0]
            self.testcases[self.curr_testcase] = []
        else:
            self.testcases[self.curr_testcase].append(filter(lambda x: x and True, column))
