from java import awt
from math import *
from jarray import array

class Graph(awt.Canvas):
    def __init__(self):
        self.function = None

    def paint(self, g):
        if self.function is None:
            return self.error(g)

        sz = self.size
        xs = range(0, sz.width, 2)

        xscale = 4*pi/sz.width
        xoffset = -2*pi

        yscale = -sz.height/2.
        yoffset = sz.height/2.

        ys = []
        for x in xs:
            x = xscale*x + xoffset
            y = int(yscale*self.function(x)+yoffset)
            ys.append(y)
        g.drawPolyline(array(xs, 'i'), array(ys, 'i'), len(xs))

    def error(self, g):
        message = "Invalid Expression"
        g.font = awt.Font('Serif', awt.Font.BOLD, 20)
        width = g.fontMetrics.stringWidth(message)

        x = (self.size.width-width)/2
        y = (self.size.height+g.fontMetrics.height)/2
        g.drawString("Invalid Expression", x, y)

    def setExpression(self, e):
        try:
            self.function = eval('lambda x: '+e)
        except:
            self.function = None
        self.repaint()


if __name__ == '__main__':
    def enter(e):
        graph.setExpression(expression.text)
        expression.caretPosition=0
        expression.selectAll()

    p = awt.Panel(layout=awt.BorderLayout())
    graph = Graph()
    p.add(graph, 'Center')

    expression = awt.TextField(text='(sin(3*x)+cos(x))/2', actionPerformed=enter)
    p.add(expression, 'South')

    import pawt
    pawt.test(p, size=(300,300))

    enter(None)
