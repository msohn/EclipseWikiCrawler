 /*
 * SPDX-FileCopyrightText: 2023 Simeon Andreev
 * SPDX-FileCopyrightText: Matthias Sohn <matthias.sohn@gmail.com>
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *
 * Based on initial version implemented by Simeon Andreev (@trancexpress) 
 * posted here https://github.com/eclipse-jdt/eclipse.jdt/issues/54#issuecomment-1431395866
 */
package test;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
    private static final Pattern IMAGE_PATTERN1 = Pattern.compile(".*\\[\\[File:([^|\\]]*).*\\].*");
    private static final Pattern IMAGE_PATTERN2 = Pattern.compile(".*\\[\\[Image:([^|\\]]*).*\\].*");

    // path of xml file downloaded from https://wiki.eclipse.org/Special:Export
    private static final Path INPUT_WIKIEXPORT_XML_FILE = Paths.get("/tmp/wikimigration/egit-wiki.xml");

    // directory containing markdown files converted from the wiki export xml file
    private static final Path OUTPUT_DIR = Paths.get("/tmp/wikimigration/markdown/egit");

    public static void main(String[] args) throws IOException {
    	Map<String, List<String> > images = getImages(INPUT_WIKIEXPORT_XML_FILE);
        String site = "https://wiki.eclipse.org/File:";
        int i = 0;
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
        						i = downloadImage(i, imageUrl, OUTPUT_DIR.resolve(path).resolve(imageName));
        						break;
        					}
        				}
        			}
        		}
        	}
        }
    }

    private static Map<String, List<String> > getImages(Path xmlFile) throws IOException {
    	Map<String, List<String> > images = new LinkedHashMap<>();
    	List<String> lines = Files.readAllLines(xmlFile);
    	String currentTitle = NO_TITLE;
    	for (String line : lines) {
    		Matcher m = TITLE_PATTERN.matcher(line);
    		if (m.matches()) {
    			currentTitle = m.group(1);
    			continue;
    		}
    		collectImageNames(images, currentTitle, IMAGE_PATTERN1.matcher(line));
    		collectImageNames(images, currentTitle, IMAGE_PATTERN2.matcher(line));
    	}
    	return images;
    }

	private static void collectImageNames(Map<String, List<String>> images, String currentTitle, Matcher m) {
		if (m.matches()) {
			String image = m.group(1).trim().replace(' ', '_');
			List<String> list = images.get(currentTitle);
			if (list == null) {
				list = new ArrayList<>();
				images.put(currentTitle, list);
			}
			list.add(image);
		}
	}

    private static int downloadImage(int downloadCount, String url, Path fileLocation) {
        Address address = new Address(url);
        Image image = new Image(address, fileLocation);
        try {
            image.download();
            if (!image.successfull()) {
            	throw new IOException("Failed to download image: " + url);
            }
            downloadCount++;
            System.out.println(String.format("%d: Done with %s -> %s", downloadCount, address.url(), fileLocation));
        } catch (IOException e) {
            System.err.println("Error when downloading " + address.url() + " : " + e.getMessage());
        }
        return downloadCount;
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
        private final Path location;

        private boolean successfull;


        public Image(Address address, Path location) {
            this.address = address;
            this.location = location;
        }

        public void download() throws IOException {
        	successfull = true;

            if (Files.exists(location)) {
                return;
            }

            URL website = new URL(address.url());
            try (ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                FileOutputStream fos = new FileOutputStream(location.toFile())) {

                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                };
            if (Files.exists(location) && Files.size(location) < MIN_BYTES) {
            	Files.delete(location);
            	successfull = false;
            }
        }

        public boolean successfull() {
            return this.successfull;
        }
    }
}
