package com.muki;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Aleksanteri on 26/11/2016.
 */

public class TwitterFeed {
    public ArrayList<Tweet> tweets = new ArrayList<Tweet>();
    public String username;

    public TwitterFeed(String username) {
        this.username = username;
    }

    public void getFeed() {
        // DEBUG
        this.tweets.add(new Tweet());
        this.tweets.get(0).postCreator = "Devs";
        this.tweets.get(0).postTitle = "Error";
        this.tweets.get(0).postDescription = "Something went wrong";

        try {
            URL url = new URL("https://twitrss.me/twitter_user_to_rss/?user=" + this.username);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(10*1000);
            connection.setConnectTimeout(10*1000);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();
            int responseCode = connection.getResponseCode();

            // Read message
            if (responseCode % 100 == 2) {
                InputStream is = connection.getInputStream();
                final int bufferSize = 1024;
                byte[] buffer = new byte[1024];
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                while (true) {
                    int count = is.read(buffer, 0, bufferSize);
                    if (count == -1) {
                        break;
                    }

                    os.write(buffer);
                }
                os.close();
                String xmlResult = new String(os.toByteArray(), "UTF-8");


                // Parse XML
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(is, null);

                int eventType = xpp.getEventType();
                ArrayList<Tweet> tweets = new ArrayList<>();
                Tweet tweet = null;
                MainActivity.RSSXMLTag currentTag = null;


                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_DOCUMENT) {
                        // Parsing start
                    } else if (eventType == XmlPullParser.START_TAG) {
                        // Find a starting tag
                        if (xpp.getName().equals("item")) {
                            tweet = new Tweet();
                            currentTag = MainActivity.RSSXMLTag.IGNORETAG;
                        }
                        else if (xpp.getName().equals("title")) {
                            currentTag = MainActivity.RSSXMLTag.TITLE;
                        }
                        else if (xpp.getName().contains("creator")) {
                            currentTag = MainActivity.RSSXMLTag.CREATOR;
                        }
                        else if (xpp.getName().equals("description")) {
                            currentTag = MainActivity.RSSXMLTag.DESCRIPTION;
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        // Find a ending tag
                        if (xpp.getName().equals("item")) {
                            // format the data here, otherwise format data in
                            // Adapter
                            this.tweets.add(tweet);
                        } else {
                            currentTag = MainActivity.RSSXMLTag.IGNORETAG;
                        }
                    } else if (eventType == XmlPullParser.TEXT) {
                        // Find text between tag
                        String content = xpp.getText();
                        content = content.trim();
                        if (tweet != null) {
                            switch (currentTag) {
                                case TITLE:
                                    if (content.length() != 0) {
                                        if (tweet.postTitle != null) {
                                            tweet.postTitle += content;
                                        } else {
                                            tweet.postTitle = content;
                                        }
                                    }
                                    break;
                                case CREATOR:
                                    if (content.length() != 0) {
                                        if (tweet.postCreator != null) {
                                            tweet.postCreator += content;
                                        } else {
                                            tweet.postCreator = content;
                                        }
                                    }
                                    break;
                                case DESCRIPTION:
                                    if (content.length() != 0) {
                                        if (tweet.postDescription != null) {
                                            tweet.postDescription += content;
                                        } else {
                                            tweet.postDescription = content;
                                        }
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    }

                    eventType = xpp.next();
                }
            }
            // If http request returns an error
            else {
                connection.getErrorStream();
                this.tweets.get(0).postTitle = "HTTP " + responseCode + " error";
            }
        }
        // If something else goes wrong
        catch (Exception e) {
            e.printStackTrace();
            this.tweets.get(0).postDescription = e.toString();
        }
    }

}
