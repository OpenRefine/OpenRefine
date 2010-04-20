from javax.servlet import Filter

class filter(Filter):
    def init(self, config):
        self.header = config.getInitParameter('header')

    def doFilter(self, req, resp, chain):
        resp.setHeader(self.header, "Yup")
        chain.doFilter(req, resp)
