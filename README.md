# Simple crawler to download images referenced in wikimedia export

Kudos to Simeon Andreev for
[posting the initial version](https://github.com/eclipse-jdt/eclipse.jdt/issues/54#issuecomment-1431395866).

This tool was created to help with migrating content from the deprecated
[wikimedia wiki of the Eclipse Foundation](https://wiki.eclipse.org) to markdown files which
can be hosted elsewhere e.g. in a GitHub wiki.

## Steps which worked for me to convert the wiki pages of the Eclipse JGit and EGit projects

- ensure all wiki pages have a common category
  - added [[Category:JGit]] to all JGit pages
  - and [[Category:EGit]] to all EGit pages
- open https://wiki.eclipse.org/Special:Export
- select pages to export manually or using the category name
- export them to an xml file per project

```bash
$ docker run -it --rm --entrypoint /bin/bash -v "$(pwd)":/wikiexport php:8.2

$ curl -sS https://getcomposer.org/installer | php -- --install-dir=/usr/local/bin --filename=composer
$ apt-get update && apt-get install -y git vim pandoc
$ git clone https://github.com/outofcontrol/mediawiki-to-gfm.git
$ cd mediawiki-to-gfm/
$ composer update --no-dev

$ ./convert.php --filename=/wikiexport/jgit-wiki.xml --output=/wikiexport/markdown/jgit/
Converted: /wikiexport/markdown/jgit/JGit
Converted: /wikiexport/markdown/jgit/JGit/FAQ
Converted: /wikiexport/markdown/jgit/JGit/User_Guide
3 files converted

$ ./convert.php --filename=/wikiexport/egit-wiki.xml --output=/wikiexport/markdown/egit/
Converted: /wikiexport/markdown/egit/EGit
Converted: /wikiexport/markdown/egit/EGit/Contributor_Guide
Converted: /wikiexport/markdown/egit/EGit/FAQ
Converted: /wikiexport/markdown/egit/EGit/Helios_Rampdown_Plan
Converted: /wikiexport/markdown/egit/EGit/Learning_Material
Converted: /wikiexport/markdown/egit/EGit/New_and_Noteworthy
Converted: /wikiexport/markdown/egit/EGit/User_Guide
7 files converted
```

## Download images referenced in the exported wiki pages

- Open the project EclipseWikiCrawler in Eclipse IDE
- configure path of xml file downloaded from https://wiki.eclipse.org/Special:Export
  by editing the constant INPUT_WIKIEXPORT_XML_FILE
- configure path of output directory containing markdown files
  converted from the wiki export xml file by editing constant OUTPUT_DIR
- run the main method of EclipseWikiCrawler to download the images
  referenced in the wikimedia export
