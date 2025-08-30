
package org.openrefine.wikibase.utils;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import com.google.refine.RefineServlet;

public class HttpClient {

    public static final String USER_AGENT = "OpenRefine-Wikibase-extension/" + RefineServlet.FULL_VERSION
            + " (https://openrefine.org; openrefine+support@discoursemail.com)";

    public static OkHttpClient getClient() {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new UserAgentInterceptor(USER_AGENT + " okhhtp/unknown")).build();
        return client;
    }

    static class UserAgentInterceptor implements Interceptor {

        private final String userAgent;

        public UserAgentInterceptor(String userAgent) {
            this.userAgent = userAgent;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            return chain.proceed(chain.request().newBuilder()
                    .header("User-Agent", userAgent)
                    .build());
        }
    }
}
