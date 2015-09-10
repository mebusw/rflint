import unittest
from rflint import RFLint

class RFLintTest(unittest.TestCase):
    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_open_and_read_lines(self):
        lint = RFLint('sample.txt')

        lint.check()

        self.assertEqual(7, lint.stat['line_count'])
        self.assertEqual(2, lint.stat['comment_line_count'])
        self.assertEqual(1, lint.stat['empty_line_count'])
        self.assertEqual(2, lint.stat['section_count'])
        self.assertEqual(2, lint.stat['row_count'])
        # self.assertEqual(2, event_count)

    def test_read_line_events(self):
        pass
        # lint = RFLint('sample.txt')
        # lint.check()
        # self.assertEqual(5, lint.stat['line_count'])


if __name__ == "__main__":
    #import sys;sys.argv = ['', 'Test.testName']
    unittest.main()
