import re


class RFLint(object):
    """docstring for RFLint"""
    
    section_prefixs = ['S', 'T', 'V', 'K']

    def __init__(self, filename):
        super(RFLint, self).__init__()
        self.filename = filename
        self.stat = {'line_count': 0, 'empty_line_count': 0, 'comment_line_count':0, 'row_count':0, 'section_count': 0}
        self.testcases = {}
        self.curr_testcase = None

    def rule(self, element, NotWithInLayer=None, ShouldNotUse=None, WillResultIn=None):
        pass

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

    def on_section(self, line, section_type):
        print line, section_type
        self.stat['section_count'] += 1

    def on_row(self, row):
        _space_splitter = re.compile(' {2,}|\t|\n')
        column = _space_splitter.split(row)
        if self.curr_testcase is None:
            self.curr_testcase = column[0]
            self.testcases[self.curr_testcase] = []
        else:
            self.testcases[self.curr_testcase].append(filter(lambda x: x and True, column))
