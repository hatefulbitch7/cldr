package org.unicode.cldr.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.unicode.cldr.util.CLDRURLS;
import org.unicode.cldr.util.CldrUtility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SurveyTool extends HttpServlet {
    static final Logger logger = SurveyLog.forClass(SurveyTool.class);

    private static final long serialVersionUID = 1L;
    private static final String FAVICON_LINK =
            "<link rel='icon' href='./favicon.ico' type='image/x-icon'>\n";

    @Override
    public final void init(final ServletConfig config) throws ServletException {
        System.out.println("🍓🍓🍓 SurveyTool.init() 🍓🍓🍓");
        logger.info("🍓🍓🍓 SurveyTool.init() 🍓🍓🍓");

        try {
            super.init(config);
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "SurveyTool.init() caught", t);
            return;
        }
    }

    /**
     * Serve the HTML for Survey Tool
     *
     * @param request
     * @param response
     * @throws IOException
     *     <p>Serve four different versions of the html page: 1. Busted/Offline 2. Starting/Waiting
     *     3. Problem (session==null) 4. Running normally
     *     <p>Plan: reduce html dynamically generated by back end (Java); leave presentation to be
     *     done by the front end (JavaScript)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        SurveyMain sm = SurveyMain.getInstance();
        PrintWriter out = response.getWriter();
        out.write("<!DOCTYPE html>\n");
        if (SurveyMain.isBusted != null || request.getParameter("_BUSTED") != null) {
            // send the waiting page if we are down/busted.
            serveWaitingPage(request, out, sm);
        } else if (sm == null
                || !SurveyMain.isSetup
                || request.getParameter("_STARTINGUP") != null) {
            serveWaitingPage(request, out, sm);
        } else {
            /*
             * TODO: clarify whether calling new WebContext and setSession is appropriate here.
             * This is how it was with the old v.jsp. However, setSession has a comment saying it should only
             * be called once. Should we get an existing ctx and status from SurveyMain?
             */
            WebContext ctx = new WebContext(request, response);
            request.setAttribute("WebContext", ctx);
            ctx.setSessionMessage(null); // ??
            ctx.setSession();
            if (ctx.session == null) {
                serveProblemNoSessionPage(request, out, ctx.getSessionMessage());
            } else {
                serveRunnningNormallyPage(request, out, sm);
            }
        }
    }

    /** Allow POST as well, used for login */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        doGet(request, response);
    }

    private void serveWaitingPage(HttpServletRequest request, PrintWriter out, SurveyMain sm)
            throws IOException {

        out.write(
                "<html lang='"
                        + SurveyMain.TRANS_HINT_LOCALE.toLanguageTag()
                        + "' class='claro'>\n");
        out.write("<!-- serveWaitingPage() -->\n");
        out.write("<head>\n");
        out.write("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>\n");
        out.write("<title>CLDR Survey Tool | Starting</title>\n");
        includeCss(request, out);
        out.write(CLDRURLS.getVettingViewerHeaderStyles() + "\n");
        try {
            includeJavaScript(request, out);
        } catch (JSONException e) {
            SurveyLog.logException(e, "Including JavaScript");
        }
        // fire up the Vue based waiting page
        out.write("</head>\n<body>\n");
        writeWaitingNavbarHtml(out);
        out.write("<div id='app'></div>");
        out.write(
                "<script>\n"
                        + "try {\n"
                        + "  cldrBundle.showPanel('retry', '#app');\n"
                        + "} catch(e) {\n"
                        + "  console.error(e);\n"
                        + "  document.getElementById('loading-err').innerText='Error: Could not CLDR ST Retry Panel. Try reloading? ' + e + '\\n' + e.stack;\n"
                        + "}\n"
                        + "</script>\n");
        out.write("</body>");
    }

    private void writeWaitingNavbarHtml(PrintWriter out) {
        out.write(
                "<div class=\"navbar navbar-fixed-top\" role=\"navigation\">\n"
                        + "  <div class=\"container\">\n"
                        + "    <div class=\"navbar-header\">\n"
                        + "      <p id=\"loading-err\" class=\"navbar-brand\">\n"
                        + "        <a href=\"http://cldr.unicode.org\">CLDR</a> SurveyTool\n"
                        + "      </p>\n"
                        + "    </div>\n"
                        + "    <div class=\"collapse navbar-collapse  navbar-right\">\n"
                        + "      <ul class=\"nav navbar-nav\">\n"
                        + "        <li><a href=\"http://cldr.unicode.org/index/survey-tool\">Help</a></li>\n"
                        + "      </ul>\n"
                        + "    </div>\n"
                        + "  </div>\n"
                        + "</div>\n");
    }

    private void serveProblemNoSessionPage(
            HttpServletRequest request, PrintWriter out, String sessionMessage) {
        out.write("<html class='claro'>\n<head class='claro'>\n");
        out.write("<!-- serveProblemNoSessionPage() -->");
        out.write("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>\n");
        out.write("<title>CLDR Survey Tool</title>\n");
        out.write("</head>\n<body>\n");
        out.write("<p class='ferrorbox'>Survey Tool is offline</p>\n");
        out.write("<div style='float: right'>\n");
        out.write(
                "  <a href='"
                        + request.getContextPath()
                        + "/login.jsp'"
                        + " id='loginlink' class='notselected'>Login…</a>\n");
        out.write("</div>\n");
        out.write("<h2>CLDR Survey Tool | Problem</h2>\n");
        out.write("<div>\n");
        out.write("<p><img src='stop.png' width='16'>" + sessionMessage + "</p>\n");
        out.write("</div>\n");
        out.write("<hr>\n");
        out.write("<p><" + SurveyMain.getObserversAndUsers() + "</p>\n");
        out.write("</body>\n</html>\n");
    }

    private void serveRunnningNormallyPage(
            HttpServletRequest request, PrintWriter out, SurveyMain sm) throws IOException {
        String lang = SurveyMain.TRANS_HINT_LOCALE.toLanguageTag();
        out.write("<html lang='" + lang + "' class='claro'>\n");
        out.write("<!-- serveRunnningNormallyPage() -->");
        out.write("<head>\n");
        out.write("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>\n");
        out.write("<title>CLDR Survey Tool</title>\n");
        out.write("<meta name='robots' content='noindex,nofollow'>\n");
        out.write("<meta name='gigabot' content='noindex'>\n");
        out.write("<meta name='gigabot' content='noarchive'>\n");
        out.write("<meta name='gigabot' content='nofollow'>\n");
        out.write(FAVICON_LINK);
        includeCss(request, out);
        out.write(CLDRURLS.getVettingViewerHeaderStyles() + "\n");
        try {
            includeJavaScript(request, out);
        } catch (JSONException e) {
            SurveyLog.logException(e, "Including JavaScript");
        }
        out.write("</head>\n");
        out.write("<body lang='" + lang + "'>\n");
        out.write("<div id='st-run-gui'>Loading...</div>\n");
        // runGui() returns a Promise.
        // But, we also want to catch exceptions in calling cldrBundle or runGui.
        out.write(
                "<script>\n"
                        + "function stRunGuiErr(e) {\n"
                        + "  console.error(e);\n"
                        + "  document.write('<h1>&#x26A0; Error: Could not load CLDR ST GUI. Try reloading?</h1> '"
                        + " + e + '\\n<hr />\\n<pre>' + (e.stack||'') + '</pre>');\n"
                        + "}\n"
                        + "try {\n"
                        + "  cldrBundle.runGui()\n"
                        + "  .then(() => {}, stRunGuiErr);\n"
                        + "} catch(e) {\n"
                        + "  stRunGuiErr(e);\n"
                        + "}\n"
                        + "</script>\n");
        out.write("</body>\n</html>\n");
    }

    private void includeCss(HttpServletRequest request, PrintWriter out) {
        final String contextPath = request.getContextPath();
        final String cb = getCacheBustingExtension(request);
        out.write(
                "<link rel='stylesheet' href='" + contextPath + "/surveytool" + cb + ".css' />\n");
        /*
         * Note: cldrForum.css is loaded through webpack
         */
        // bootstrap.min.css -- cf. bootstrap.min.js elsewhere in this file
        out.write(
                "<link rel='stylesheet' href='//stackpath.bootstrapcdn.com/bootswatch/3.1.1/spacelab/bootstrap.min.css' />\n");
        out.write(
                "<link rel='stylesheet' href='"
                        + contextPath
                        + "/css/redesign"
                        + cb
                        + ".css' />\n");
    }

    private static final String DD_CLIENT_TOKEN = System.getenv("DD_CLIENT_TOKEN");
    private static final String DD_CLIENT_APPID = System.getenv("DD_CLIENT_APPID");
    private static final String DD_GIT_COMMIT_SHA = System.getenv("DD_GIT_COMMIT_SHA");
    private static final String DD_ENV = System.getProperty("dd.env", "");

    /** if DD_CLIENT_TOKEN is set, set these variables so index.js can pick them up. */
    public static void includeMonitoring(Writer out) throws IOException {
        if (DD_CLIENT_TOKEN != null && !DD_CLIENT_TOKEN.isEmpty()) {
            out.write(
                    String.format(
                            "<script>\n"
                                    + "window.dataDogClientToken='%s';\n"
                                    + "window.dataDogAppId='%s';\n"
                                    + "window.dataDogEnv='%s';\n"
                                    + "window.dataDogSha='%s';\n"
                                    + "</script>\n",
                            DD_CLIENT_TOKEN, DD_CLIENT_APPID, DD_ENV, DD_GIT_COMMIT_SHA));
        } else {
            out.write("<script>window.dataDogClientToken='';</script>\n");
        }
    }

    static private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final class STManifest {
        public String jsfiles[];
    }

    /**
     * Write the script tags for Survey Tool JavaScript files
     *
     * @param request the HttpServletRequest
     * @param out the Writer
     * @throws IOException
     * @throws JSONException
     */
    public static void includeJavaScript(HttpServletRequest request, Writer out)
            throws IOException, JSONException {
        includeMonitoring(out);

        // use WebPack-built manifest.json to include all chunks.
        // ideally this would all come from a static .html file built by WebPack.
        // TODO https://unicode-org.atlassian.net/browse/CLDR-17353
        try (final InputStream is = request.getServletContext().getResourceAsStream("dist/manifest.json");
            final Reader r = new InputStreamReader(is, StandardCharsets.UTF_8);) {
                for(final String f : gson.fromJson(r, STManifest.class).jsfiles) {
                    out.write("<script src=\"" + request.getContextPath() + "/dist/" + f.toString() + "\"></script>\n");
                }
        }

        includeJqueryJavaScript(request, out);
        includeCldrJavaScript(request, out);
    }



    private static void includeJqueryJavaScript(HttpServletRequest request, Writer out)
            throws IOException {
        // Per https://en.wikipedia.org/wiki/JQuery#Release_history --
        // jquery 3.5.1: May 4, 2020
        out.write(
                "<script src='//ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js'></script>\n");

        // Per https://en.wikipedia.org/wiki/JQuery_UI#Release_history --
        // jquery-ui 1.12.1: Sep 14, 2016 -- that's the newest
        // Per https://jqueryui.com/ -- Current stable "v1.12.1 jQuery 1.7+"
        out.write(
                "<script src='//ajax.googleapis.com/ajax/libs/jqueryui/1.12.1/jquery-ui.min.js'></script>\n");
    }

    private static void includeCldrJavaScript(HttpServletRequest request, Writer out)
            throws IOException {
        final String prefix = "<script src='" + request.getContextPath() + "/js/";
        final String tail = "'></script>\n";
        // Autosize 4.0.2 (2018-04-30 per changelog.md), see http://www.jacklmoore.com/autosize
        out.write(prefix + "autosize.min.js" + tail);
        out.write(prefix + "bootstrap.min.js" + tail);
    }

    /**
     * The cache-busting filename extension, like "._b7a33e9fe_", to be used for those http requests
     * that employ the right kind of server configuration (as with nginx on the production server)
     */
    private static String cacheBustingExtension = null;

    /**
     * Get a string to be added to the filename, like "._b7a33e9f_", if we're responding to the kind
     * of request we get with nginx; else, get an empty string (no cache busting).
     *
     * <p>If we're running with a reverse proxy (nginx), use "cache-busting" to make sure browser
     * uses the most recent JavaScript files.
     *
     * <p>Change filename to be like "CldrStAjax._b7a33e9f_.js", instead of adding a query string,
     * like "CldrStAjax.js?v=b7a33e9fe", since a query string appears sometimes to be ignored by
     * some browsers. The server (nginx) needs a rewrite rule like this to remove the hexadecimal
     * hash:
     *
     * <p>rewrite ^/(.+)\._[\da-f]+_\.(js|css)$ /$1.$2 break;
     *
     * <p>Include underscores to avoid unwanted rewrite if we had a name like "example.bad.js",
     * where "bad" could be mistaken for a hexadecimal hash.
     *
     * @return a (possibly empty) string to be added to the filename
     */
    private static String getCacheBustingExtension(HttpServletRequest request) {
        if (request.getHeader("X-Real-IP") == null) {
            /*
             * Request wasn't made through nginx? Leave cacheBustingExtension alone, to enable
             * both kinds of request at the same time (with/without nginx) for debugging
             */
            return "";
        }
        if (cacheBustingExtension == null) {
            final String hash = CldrUtility.getCldrBaseDirHash();
            if (hash == null || !hash.matches("[0-9a-f]+")) {
                cacheBustingExtension = "";
            } else {
                cacheBustingExtension = "._" + hash.substring(0, 8) + "_";
            }
        }
        return cacheBustingExtension;
    }
}
