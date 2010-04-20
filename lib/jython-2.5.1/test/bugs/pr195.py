# PR#195 -- inclusion of multiple copies of a method, due to interface
# appearing multiple times in a hierarchy.

from javax.swing import AbstractListModel, ComboBoxModel
# Note that AbstractListModel implements ListModel, ComboBoxModel extends
# ListModel.  This would cause Jython to include
# (Object)ListModel.getElementAt(int) twice in the proxy class, which is
# illegal.

class Model(AbstractListModel, ComboBoxModel):
    pass
