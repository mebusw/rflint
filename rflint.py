class RFLint(object):
    """docstring for RFLint"""
    
    section_prefixs = ['S', 'T', 'V', 'K']

    def __init__(self, filename):
        super(RFLint, self).__init__()
        self.filename = filename
        self.stat = {'line_count': 0, 'empty_line_count': 0, 'comment_line_count':0, 'row_count':0, 'section_count': 0}


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
                    self.on_row(line)

    def on_section(self, line, section_type):
        print line, section_type
        self.stat['section_count'] += 1

    def on_row(self, line):
        print line
        self.stat['row_count'] += 1
