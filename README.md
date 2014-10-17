nuxeo-imagemetadata-utils
=========================
Currrent version: 1.0.1 - 2014-10-17

## About - Requirements
`nuxeo-imagemetadata-utils` is a plug-in for the `nuxeo platform`. It allows to extract metadata stored in pictures and store these information in the document, for easy search, display and reporting. It uses the `ìm4java` tool for this purpose, which, itself, encapsulates calls to `ImageMagick` and, possibly, `ExifTool` (ExifTool is not used in current version.)

**Requirements**: In current version, `nuxeo-imagemetadata-utils` only uses the `ImageMagick` part of `ìm4java`: `ImageMagick` must be installed on your server. This is most likely the case, since `nuxeo` already requires `ImageMagick` (creation of thumbnails, creation of previews, ...)

## Table of Content

* [Usage](#usage)
  * [Parameters](#parameters)
  * [Importing the Operation in your Studio Project](#importing-the-operation-in-your-studio-project)
  * [Example of Use with Studio](#example-of-use-with-studio)
* [Installation](#installation)
  * [As Marketplace Package](#using-the-marketplace-package-available-in-the-releases-section-of-this-github-repository)
  * [Manual Installation](#manual-installation)
* [Building the Plugin](#building-the-plugin)
* [Versions](#versions)
* [Third Party Tools Used](#third-party-tools-used)
* [License](#license)
* [About Nuxeo](#about-nuxeo)

## Usage
#### Parameters
The plug-in provides the `Save Picture Metadata in Document`, Automation Operation installed in the `Document` topic. This operation expects 3 optional parameters: `xpath`, `properties` and `save`.

* `xpath` is the path to the binary, in the document, holding then picture. It is set by default to `file:content`, which means the default main binary
* When the `save` box is checked then the document will be automatically saved. Not checking this box is interesting when the next operations, for example, will also update some fields, so we want some time to avoid saving the document in the database, triggering events, etc.
* The `properties` parameter is a list a `key=value` elements (separated by a line), where `key` is the XPATH of a field and `value`is the exact name (case sensitive) of a picture metadata field, as returned by the `identify -verbose` command of `ImageMagick` (sub-properties use a colon as separator: `image statistics:Overall:standard deviation` for example). You could use something like:<br/>
```
dc:format=Format
a_schema_prefix:a_field=Units
another_schema_prefix:some_field=Number pixels
my_channel:red=Channel depth:red
my_channel:red=Channel depth:green
my_channel:red=Channel depth:blue
... etc ...
```
In this example, the plugin will store in `dc:format` the value of the `Format` field, in `my_channel:red` the value of `Channel depth:red`, etc.

 **Special values for `properties`**<br/>
 * `all`: If a `value` is set to "all" (`dc:description=all` for example), then the raw, string, value of `identify -verbose` is returned. This is a good way to check what kind of values you can expect. The whole values could also be stored in a string field and full-text indexed.
 * If `properties` is left empty, then the default behavior is to extract some informations and store them in the `image_metadata` schema (prefix `imd`.)
   * This schema is provided by Nuxeo and is available by default in the `Picture` document.
   * If you declare a custom document type, don't herit from `image_metadata` and let `properties` empty, then the plug-in does nothing (and does not fire an error), letting the document unchanged
   * The plug-in stores:
     * The width in `imd:pixel_xdimension`
     * The height in `imd:pixel_ydimension`
     * The colorspace in `imd:color_space`
     * The X resolution, as Dot Per Inch, in `imd:xresolution`
     * The Y resolution, as Dot Per Inch, in `imd:yresolution`
       * NOTE: The resolution is converted in Dots Per Inch if needed (for example, a PNG, storing the information as PIxelsPerCentimeter;)


#### Importing the Operation in your Studio Project
Because this operation is not part of the default platform, it is not available by default in Studio, you must add its JSON definition in the `Automation Operations` registry of your Studio project:

* Copy the JSON definition of the operation (see below)
* In Studio, go to "Settings & Versioning" > "Registries" > "Automation Operations"
* Paste the JSON definition:
  * If this operation is the only one you are importing, then you can just start the registry with `{"operations": [`, then copy the JSON definition, and then add `]}`, so you would have something like:<br>
  ```
  {
    "operations": [
      {
        "id" : "ExtractMetadataInDocument",
        "label" : "Save Picture Metadata in Document",
        . . . etc . . .
      }
    ]
  }
  ```
  * If you already have other custom operations, just add this to the list( don't forget the `,` to separate the operations)

**Getting the JSON Definition of the Operation**
* Install the plug-in (see below, "Installation")
* Once the plug-in is installed and the server running:
  * Login as an administrator
  * Then, go to {your-server:port}/nuxeo/site/automation/doc
  * Find the operation (just do a search on the page for `Save Picture Metadata in Document` for example)
  * Click the "JSON Definition" link
  * Copy the JSON definition
  * WARNING: If the description of the operation contains lines, remove them manually of saving the definition will fail.



#### Example of Use with Studio

**Update Metadata when a Picture is Created/Modified**<br/>
We want to update the metadata every time the binary file is modified. Which means, basically, when the document is created or when the user replaces the existing file with another.

To achieve this, we may want to install event handlers for the "Document created" and the "Document modified" events, but this will not work as expected because the default handling of metadata by nuxeo will override our changes: We want to trigger our operation *after* the default behavior is done. The point is that once default behavior is done handling the picture, it fires the `pictureViewsGenerationDone` event. So, we want to call our own operation for this event: This way, we are 100% sure that we can change the metadata. Since the `pictureViewsGenerationDone` event is not (at the time this writing) an event listed by Studio, we must add it to the registry. Here is the full sequence, including the Automation Chain to use:

1. Add the `pictureViewsGenerationDone` event to the "Core Events" registry
  * See http://doc.nuxeo.com/pages/viewpage.action?pageId=8683799
  * Go to Settings & Versioning > Registries > Core Events
  * Paste the following:
  ```
  {
    events: {
      pictureViewsGenerationDone: "pictureViewsGenerationDone"
    }
  }
  ```
  * Save
2. Create a new Event Handler:
  * Go to Automation > Event Handlers and click "New"
  * Name your event as you wish. For example, just `pictureViewsGenerationDone`
  * In "Event Handler Definition", scroll the list of events, you will find your new `pictureViewsGenerationDone` at the end of the list. Select it
  * In "Event Handler Enablement", just set "Current document is" to "Mutable document" (because we don't want to calculate the data on a version for example, Nuxeo would trigger an error)
  * In "Event Handler Execution", click the "create" button and name the Automation Chain which will be called for this event. For example, `Picture_GetMetadataAndSave`
3. Create the Automation Chain
  * Previous step has already open the `Picture_GetMetadataAndSave` automation chain for you
  * You chain is just:
  ```
  Fetch > Context Document(s)
  Document > Save Picture Metadata in Document
    xpath: file:content
    properties:
    save: checked
  ```

**Display the Information**<br/>
If you used the default parameters, the data is stored in the `image_metadata` schema, prefix `imd`. in your layouts, you can just select this schema in the "Widgets by Properties" drop down, and drag-drop the fiels you want to use.

## Installation

#### Using the Marketplace Package Available in the `releases` Section of this GitHub Repository
The name is `nuxeo-imagemetadata-utils-mp-{version}-SNAPSHOT.zip`. You should use the latest version (see [here](#versions)).

_NOTE_: Even if the version number is 5.9.6, this plug-in works (and has been tested) with no problem using nuxeo 5.9.5.

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
* From the `releases` tab of this repository, download the `ManualInstallation` file. Extract the `.zip`. It contains 2 files: `nuxeo-imagemetadata-utils-plugin-{version}-SNAPSHOT.jar` and `im4java-1.4.0.jar`
* Stop `nuxeo` server
* Install:
  * `nuxeo-imagemetadata-utils-plugin-{version}-SNAPSHOT.jar` in `{server-path}/nxserver/bundles`
  * And `im4java-1.4.0.jar` in `{server-path}/nxserver/lib`
* Start `nuxeo` server


## Building the Plugin
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
* The Marketplace Package is in `nuxeo-imagemetadata-utils/nuxeo-imagemetadata-utils-mp/target`, its name is `nuxeo-imagemetadata-utils-mp-{version}.zip`.

If you want to import the source code in Eclipse, then after the first build, `cd nuxeo-imagemetadata-utils-plugin` and `mvn eclipse:eclipse`. Then, in Eclipse, choose "File" > "Import...", select "Existing Projects into Workspace" navigate to the `nuxeo-imagemetadata-utils-plugins` folder and select this folder.

## Versions

* **First release** was tagged "5.9.6-SNAPSHOT"
  * Which was a mistake because, by using nuxeo's version, it did not allow incrementing the plug-in's own version easily
  * So, this 5.9.6-SNAPSHOT must be read as "version 1.0.0-SNAPSHOT"

* **Version 1.0.1** (2010-10-17)
  * Allows to handle a new document not yet saved, so the `Save Picture Metadata in Document` operation can be called from the `Àbout to Create` Event Handler
  * Change version numbering


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
