/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.appengine;

import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.params.HttpParams;

public class AppEngineClientConnectionManager implements ClientConnectionManager {

    private SchemeRegistry schemes;

    class NoopSocketFactory implements SocketFactory {
        public Socket connectSocket(Socket sock, String host, int port, InetAddress addr, int lport, HttpParams params) {
            return null;
        }

        public Socket createSocket() {
            return null;
        }

        public boolean isSecure(Socket sock) {
            return false;
        }
    }

    public AppEngineClientConnectionManager() {
        SocketFactory noop_sf = new NoopSocketFactory();
        schemes = new SchemeRegistry();
        schemes.register(new Scheme("http",  noop_sf, 80));
        schemes.register(new Scheme("https", noop_sf, 443));
    }

    public void closeExpiredConnections() {
        return;
    }

    public void closeIdleConnections(long idletime, TimeUnit tunit) {
        return;
    }

    public ManagedClientConnection getConnection(HttpRoute route, Object state) {
        return new AppEngineClientConnection(route, state);
    }

    public SchemeRegistry getSchemeRegistry() {
        return schemes;
    }

    public void releaseConnection(ManagedClientConnection conn, long valid, TimeUnit tuint) {
        return;
    }

    public ClientConnectionRequest requestConnection(final HttpRoute route, final Object state) {
        return new ClientConnectionRequest() {
            public void abortRequest() {
                return;
            }

            public ManagedClientConnection getConnection(long idletime, TimeUnit tunit) {
                return AppEngineClientConnectionManager.this.getConnection(route, state);
            }
        };
    }

    public void shutdown() {
        return;
    }
}
