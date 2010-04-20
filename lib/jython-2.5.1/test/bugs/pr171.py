# PR#171.  pdb's "c" command fails.  The underlying problem is that frame
# objects didn't have a writeable f_trace attribute.

import sys

try: 1/0
except: frame = sys.exc_info()[2].tb_frame

del frame.f_trace
