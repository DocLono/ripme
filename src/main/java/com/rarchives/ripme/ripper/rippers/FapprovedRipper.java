package com.rarchives.ripme.ripper.rippers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.rarchives.ripme.ripper.AlbumRipper;
import com.rarchives.ripme.ui.RipStatusMessage.STATUS;

public class FapprovedRipper extends AlbumRipper {

    private static final String DOMAIN = "fapproved.com",
                                HOST   = "fapproved";
    private static final Logger logger = Logger.getLogger(FapprovedRipper.class);

    public FapprovedRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public boolean canRip(URL url) {
        return (url.getHost().endsWith(DOMAIN));
    }

    @Override
    public URL sanitizeURL(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://fapproved\\.com/users/([a-zA-Z0-9\\-_]{1,}).*$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return new URL("http://fapproved.com/users/" + m.group(1));
        }
        throw new MalformedURLException("Expected username in URL (fapproved.com/users/username and not " + url);
    }
    @Override
    public void rip() throws IOException {
        int page = 0;
        String url, user = getGID(this.url);
        boolean hasNextPage = true;
        while (hasNextPage) {
            page++;
            url = "http://fapproved.com/users/" + user + "/images?page=" + page;
            this.sendUpdate(STATUS.LOADING_RESOURCE, url);
            logger.info("    Retrieving " + url);
            Document doc = Jsoup.connect(url)
                                     .ignoreContentType(true)
                                     .get();
            for (Element image : doc.select("div.actual-image img")) {
                String imageUrl = image.attr("src");
                if (imageUrl.startsWith("//")) {
                    imageUrl = "http:" + imageUrl;
                }
                addURLToDownload(new URL(imageUrl));
            }
            if ( (doc.select("div.pagination li.next.disabled").size() != 0)
              || (doc.select("div.pagination").size() == 0) ) {
                break;
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                logger.error("[!] Interrupted while waiting to load next album:", e);
                break;
            }
        }
        waitForThreads();
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://[w.]*fapproved.com/users/([a-zA-Z0-9\\-_]{3,}).*$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }
        throw new MalformedURLException("Fapproved user not found in " + url + ", expected http://fapproved.com/users/username/images");
    }

}
