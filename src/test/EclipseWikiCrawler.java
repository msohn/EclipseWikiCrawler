package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EclipseWikiCrawler {

    private static final String NO_TITLE = "no_title";
    private static final Pattern TITLE_PATTERN = Pattern.compile(".*<title>(.*)</title>.*");
    private static final Pattern IMAGE_PATTERN = Pattern.compile(".*\\[\\[Image:([^\\]]*)\\]\\].*");

    public static void main(String[] args) throws IOException {
    	Map<String, List<String> > images = getImages();
        String site = "https://wiki.eclipse.org/File:";
        for (Entry<String, List<String>> entry : images.entrySet()) {
        	String title = entry.getKey();
        	String path = title.replace(' ', '_');
        	int indexOfLastSlash = path.lastIndexOf('/');
        	if (indexOfLastSlash == -1) {
        		path = "";
        	} else {
        		path = path.substring(0, indexOfLastSlash);
        	}
        	Set<String> imageNames = new LinkedHashSet<>(entry.getValue());
        	for (String imageName : imageNames) {
        		URL url = new URL(site + imageName);
        		try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
        			String inputLine;
        			while ((inputLine = in.readLine()) != null) {
        				int srcStartIndex = inputLine.indexOf("src=\"/images/");
        				if (srcStartIndex != -1) {
        					int srcEndIndex = inputLine.indexOf(imageName, srcStartIndex);
        					if (srcEndIndex != -1) {
        						String srcPart = inputLine.substring(srcStartIndex, srcEndIndex);
        						String src = srcPart.substring("src=\"".length());
        						String imageUrl = "https://wiki.eclipse.org/" + src + imageName;
        						downloadImage(imageUrl, "/data/git/eclipse/eclipse.jdt/wiki/" + path +"/" + imageName);
        						break;
        					}
        				}
        			}
        		}
        	}
        }
    }

    private static Map<String, List<String> > getImages() throws IOException {
    	Map<String, List<String> > images = new LinkedHashMap<>();
    	String xmlFile = "/tmp/wikiexport/Eclipsepedia-20230215110023.xml";
    	List<String> lines = Files.readAllLines(Paths.get(xmlFile));
    	String currentTitle = NO_TITLE;
    	for (String line : lines) {
    		Matcher m = TITLE_PATTERN.matcher(line);
    		if (m.matches()) {
    			currentTitle = m.group(1);
    			continue;
    		}
    		m = IMAGE_PATTERN.matcher(line);
    		if (m.matches()) {
    			String image = m.group(1);
    			List<String> list = images.get(currentTitle);
    			if (list == null) {
    				list = new ArrayList<>();
    				images.put(currentTitle, list);
    			}
				list.add(image);
    		}
    	}
    	return images;
    }

    private static void downloadImage(String url, String fileLocation) {
        Address address = new Address(url);
        Image image = new Image(address, fileLocation);
        try {
            image.download();
            if (!image.successful()) {
            	throw new IOException("Failed to download image: " + url);
            }
            System.out.println("Done with " + address.url() + " -> " + fileLocation);
        } catch (IOException e) {
            System.err.println("Error when downloading " + address.url() + " : " + e.getMessage());
        }
    }

    private static class Address {

        private final String url;

        public Address(String url) {
            this.url = url;
        }

        public String url() {
            return url;
        }
    }

    private static class Image {

        private static final int MIN_BYTES = 1;

        private final Address address;
        private final String location;

        private boolean successful;


        public Image(Address address, String location) {
            this.address = address;
            this.location = location;
            successful = false;
        }


        public void download() throws IOException {
            successful = true;

            File imageOnDisk = new File(location);
            if (imageOnDisk.exists()) {
                return;
            }

            URL website = new URL(address.url());
            try (
                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                FileOutputStream fos = new FileOutputStream(imageOnDisk)) {

                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }


            if (imageOnDisk.exists() && (imageOnDisk.length() < MIN_BYTES)) {
                imageOnDisk.delete();
                successful = false;
            }
        }

        public boolean successful() {
            return successful;
        }
    }

}