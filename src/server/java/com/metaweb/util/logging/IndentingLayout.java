package com.metaweb.util.logging;

/*
 * Copyright (c) Massachusetts Institute of Technology, 2007 
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *    Original code:  http://simile.mit.edu/repository/tracer/trunk/
 */

import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * This is a special Log4j log formatter that is capable of reacting on special log messages
 * and 'indent' the logs accordingly. This is very useful to visually inspect a debug log
 * and see what calls what. An example of logs are "> method()" and "< method()" where > and <
 * are used to indicate respectively "entering" and "exiting".
 */
public class IndentingLayout extends Layout {

    protected static final int CONTEXT_SIZE = 25;
    protected static final long MAX_DELTA = 10000;

    protected final StringBuffer buf = new StringBuffer(256);
    protected Calendar calendar = Calendar.getInstance();
    protected long previousTime = 0;
    protected int indentation = 0;

    public void activateOptions() {
        // no options at this time
    }

    public String format(LoggingEvent event) {
        String message = event.getRenderedMessage();
        if (message == null) return "";
        if (message.length() < 2) return message;
        
        char leader = message.charAt(0);
        char secondLeader = message.charAt(1);
        if ((leader == '<') && (secondLeader == ' ') && (this.indentation > 0)) this.indentation--;

        // Reset buf
        buf.setLength(0);

        Date date = new Date();
        long now = date.getTime();
        calendar.setTime(date);

        long delta = 0;
        if (previousTime > 0) {
            delta = now - previousTime;
        }
        previousTime = now;

//        if ((previousTime == 0) || (delta > MAX_DELTA)) {
//            buf.append('\n');
//            indentation = 0; // reset indentation after a while, as we might
//            // have runaway/unmatched log entries
//        }

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour < 10) buf.append('0');
        buf.append(hour);
        buf.append(':');

        int mins = calendar.get(Calendar.MINUTE);
        if (mins < 10) buf.append('0');
        buf.append(mins);
        buf.append(':');

        int secs = calendar.get(Calendar.SECOND);
        if (secs < 10) buf.append('0');
        buf.append(secs);
        buf.append('.');

        int millis = (int) (now % 1000);
        if (millis < 100) buf.append('0');
        if (millis < 10) buf.append('0');
        buf.append(millis);

        buf.append(" [");
        String context = ((String) event.getMDC("LogEvent"));
        if (context == null) {
            context = event.getLoggerName();
        }
        if (context.length() < CONTEXT_SIZE) {
            pad(buf, CONTEXT_SIZE - context.length(), ' ');
            buf.append(context);
        } else {
            buf.append("..");
            buf.append(context.substring(context.length() - CONTEXT_SIZE + 2));
        }
        buf.append("] ");

        pad(buf, indentation, ' ');

        buf.append(message);

        buf.append(" (");
        buf.append(delta);
        buf.append("ms)\n");

        if ((leader == '>') && (secondLeader == ' ')) indentation++;

        return buf.toString();
    }

    private void pad(StringBuffer buffer, int pads, char padchar) {
        for (int i = 0; i < pads; i++) {
            buf.append(padchar);
        }
    }

    public boolean ignoresThrowable() {
        return true;
    }
}
