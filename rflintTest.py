import unittest
from rflint import RFLint

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

class RFLintTest(unittest.TestCase):
    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_open_and_read_lines(self):
        lint = RFLint('sample.txt')
        lint.rule('AnyKeyword', NotWithInLayer='ElementInteraction', ShouldNotUse='Selenium2LibKeywords', WillResultIn='ERROR')
        
        lint.check()

        self.assertEqual(8, lint.stat['line_count'])
        self.assertEqual(2, lint.stat['comment_line_count'])
        self.assertEqual(1, lint.stat['empty_line_count'])
        self.assertEqual(2, lint.stat['section_count'])
        self.assertEqual(3, lint.stat['row_count'])
        # self.assertEqual(2, event_count)
        print lint.testcases

    def test_read_line_events(self):
        pass
        # lint = RFLint('sample.txt')
        # lint.check()
        # self.assertEqual(5, lint.stat['line_count'])


if __name__ == "__main__":
    #import sys;sys.argv = ['', 'Test.testName']
    unittest.main()
