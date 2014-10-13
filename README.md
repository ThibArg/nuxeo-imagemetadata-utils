nuxeo-imagemetadata-utils
=========================

## About - Requirements
`nuxeo-imagemetadata-utils` is a plug-in for the `nuxeo platform`. It allows to extract metadata stored in pictures and store these information in the document, for easy search, display and reporting. It uses the `ìm4java` tool for this purpose, which, itself, encapsulates calls to `ImageMagick` and, possibly, `ExifTool` (ExifTool is not used in current version.)

**Requirements**: In current version, `nuxeo-imagemetadata-utils` only uses the `ImageMagick` part of `ìm4java`: `ImageMagick` must be installed on your server. This is most likely the case, since `nuxeo` already requires `ImageMagick` (creation of thumbnails, creation of previews, ...)

## Table of Content

* [Installation](#installation)
  * [As Marketplace Package](#using-the-marketplace-package-available-in-the-releases-section-of-this-github-repository)
  * [Manual Installation](#manual-installation)
  * [Building the Plugin](#building-the-plugin)
* [Third Party Tools Used](#third-party-tools-used)
* [License](#license)
* [About Nuxeo](#about-nuxeo)

## Installation

#### Using the Marketplace Package Available in the `releases` Section of this GitHub Repository
The name is `nuxeo-imagemetadata-utils-mp-{version}.zip`, with `{version}` equals to 5.9.5 for example.

Download this .zip Marketplace Package and install it on your server:
* Either from the Admin. Center:
  * As administrator (Administrator/Administrator by default), in the Admin Center, click on the `Update Center` left tab.
  * Click on the `Local packages` tab.
  * Click on the `Upload a package` button.
    * An upload form is displayed just below the tabs.
    * Click on the `Browse button` to select the package .zip package file.
    * Click on the `Upload` button.
    * Once the .zip is uploaded, install it by clicking on the `Install` link
    * A confirmation page is displayed, click the `Start` button, and follow the instruction if needed
* Or from the `nuxeoctl` command line:
  * Make sure the server is not running, stop it if needed
  * Then you can `nuxeoctl mp-install /path/to/the/zip/package` and answer "yes" to the confirmation.
  * (then, restart the server)


#### Manual Installation
You can manually install the plug-in:
* From the `releases` tab of this repository, download the `manual-install-{version}.zip` file (where `{version}` is 5.9.5 for example). Extract the `.zip`. It contains 2 files: `nuxeo-imagemetadata-utils-plugin-{version}.jar` and `im4java-1.4.0.jar`
* Stop `nuxeo` server
* Install:
  * `nuxeo-imagemetadata-utils-plugin-{version}.jar` in `{server-path}/nxserver/bundles`
  * And `im4java-1.4.0.jar` in `{server-path}/nxserver/lib`
* Start `nuxeo` server

#### Building the Plugin
You can also download the source code and compile the plug-in. Which is what you will do if you want to change, adapt, etc.
Assuming [`maven`](http://maven.apache.org) (min. 3.2.1) is installed on your computer:
```
# Clone the GitHub repository
cd /path/to/where/you/want/to/clone/this/repository
git clone https://github.com/ThibArg/nuxeo-imagemetadata-utils
# Compile
cd nuxeo-imagemetadata-utils
mvn clean install
```

* The plug-in is in `nuxeo-imagemetadata-utils/nuxeo-imagemetadata-utils-plugin/target/`, its name is `nuxeo-imagemetadata-utils-plugin-{version}.jar`.
* The Marketplace Package is in `nuxeo-imagemetadata-utils/nuxeo-imagemetadata-utils-mp/target`, its name is `nuxeo-imagemetadata-utils-mp-5.9.5.zip`.

If you want to import the source code in Eclipse, you can use `mvn eclipse:eclipse` (after having `mvn install`). Then, in Eclipse, choose "File" > "Import...", select "Existing Projects into Workspace" navigate to the `nuxeo-imagemetadata-utils-plugins` folder and select this folder.

## Third Party Tools Used
* **`im4java`**<br/>
`im4java` (http://im4java.sourceforge.net) is used as the main tool to extract the metadata from the images. It is a very good and powerful tool which saves a lot of development time by having already everything requested.<br/>
`im4java` is licensed under the LGPL


## License
(C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.

All rights reserved. This program and the accompanying materials
are made available under the terms of the GNU Lesser General Public License
(LGPL) version 2.1 which accompanies this distribution, and is available at
http://www.gnu.org/licenses/lgpl-2.1.html

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
Lesser General Public License for more details.

Contributors:
Thibaud Arguillere (https://github.com/ThibArg)

## About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software platform for enterprise content management](http://www.nuxeo.com) and packaged applications for Document Management, Digital Asset Management and Case Management. Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.

More information on: <http://www.nuxeo.com/>/Users/thibaud/Downloads/im4java-master/COPYING.LIB