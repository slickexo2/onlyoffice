#eXo Onlyoffice add-on

This add-on allow editing office documents directly in eXo Platform using Onlyoffice Documents Server. Thanks to added menu items, in action bar of Documents explorer, users of eXo Platform can switch to editor in file preview of any supportable office document. As for now, the add-on offers support of following formats: 
* Office Open XML (Microsoft Office 2007 and later):
** Word (.docx)
** Excel (.xlsx) 
** PowerPoint (.pptx) 
* Open Document Format (OnlyOffice and LibreOffice):
** Text (.odt)
** Spreadsheet (.ods)
** Presentation (.odp)
 
The Document Server should be [installed](http://helpcenter.onlyoffice.com/server/document.aspx) on reachable network. Onlyoffice offers native installers for Windows and Linux, they also have [Docker installation](http://helpcenter.onlyoffice.com/server/docker/document/docker-installation.aspx) which will be described below in details. 

![Video]()

##Usage

Since add-on installed, it adds two menu items in Document explorer action bar: "Edit In Onlyoffice" and "Close Onlyoffice". These menu items available in file preview mode (need open a file in the explorer) and also work for large files when preview not possible. 

![Editor in Action bar](/docs/images/action_bar.png)

After user will choose to open an editor for current file, it will be laoded instead of file preview on the explorer page. As Onlyoffice requires some time to download the document content from eXo to Document Server and then prepare an UI, users will need wait for seconds before starting to work. Large files may require more time for loading (e.g. spreadsheets with lot of cells). 

![Editor open](/docs/images/editor.png)

When users start to edit a document it's important to understand how Onlyoffice editor works. All modifications will go to a temporal storage in their Documents Server and only after all editors will be closed, in about 10 seconds, the file will be updated in eXo Platform. Thus, while document is open in the editor, all users opened it see actual state, but if look at the preview in eXo it will show a state before the editing session start.
To finish editing the document, user needs to close the editor by menu "Close Onlyoffice".

When several users edit the same document, it is required to make extra operations to get synced with others. In single user mode all editings will be applied immediately to the document temporal storage. But when multiple users open the document and someone edited something in it, edited part of the document will be locked for others until this user save the document explicitly using Save menu. After document saved current user will see also changes of others (in other paragraph, table cell etc). But others, in order to see remote modifications, will need also to click Save button in their editors, even if they haven't changed anything. Onlyoffice shows a popover on the Save button by default when document changed by others, it lets user to see need of such synchronization.

![Co-editing](/docs/images/coediting.png)

##Installation

###Onlyoffice Document Server

Onlyoffice Document Server can be [installed](http://helpcenter.onlyoffice.com/server/docker/document/docker-installation.aspx) in Docker container from ready image with pre-cpnfigured default. While this add-on has no constraints on how Onlyoffice installed, Docker way is a simplest and strightforward way to add Document Office to you infra, for both development or production. Document Server is an Open Source Software, thus if you'll need to customize it, it's always possible to get the sources and make your Docker image.

When you have ready Docker installed, you need a single command to start the Document Server on-premise:

    sudo docker run -i -t -d -p 80:80 onlyoffice/documentserver

The Document Server needs only port 80 by default, but you also can run in on HTTPS. Follow its guides for more detailed setup.

To check you have running Document Server successfuly, open a page on its address [http://DOCUMENT\_SERVER\_HOST/OnlineEditorsExample/], it will show you their simple demo where you can try upload a document, or use their sample, and try to edit online in your browser. When you see it works well, you are ready to install the add-on in eXo Platform.

###eXo Platform Add-on
After you successfuly installed Onlyoffice Document Server, you need install Onlyoffice add-on to eXo Platform server. It should be Platform version 4.3 or higher. 

If you want use released binaries from eXo Catalog, simple run command in root of the server:

    > ./addon install onlyoffice

When building from sources, then go to /packaging/target folder of the project and extract exo-onlyoffice-packaging.zip to root of your server. You also may setup local repository for eXo Addon manager if plan development and frequent installations, and use this archive as a local package.

##Configuration

Onlyoffice add-on need to know an adress of your Document Server and eXo Platform server, and it's also required to point a protocol scheme to use (http or https). Add following lines to your `exo.properties`:

    onlyoffice.documentserver.host=YOUR_DOCUMENT_SERVER_HOST
    onlyoffice.documentserver.schema=http

    onlyoffice.server.host=YOUR_EXO_SERVER_HOST
    onlyoffice.server.schema=http

Where `YOUR_DOCUMENT_SERVER_HOST` is an IP or host name with port of Document Server and `YOUR_EXO_SERVER_HOST` host and port of your eXo Platform.


##Security

Onlyoffice Document Server standalone doesn't offer a user authorization or such integration with external identities. As a result it's required to place it in secure network and for production it will be mandatory to run via [HTTPS](http://helpcenter.onlyoffice.com/server/docker/document/docker-installation.aspx#RunningHTTPS) or even use [strong SSL security](https://raymii.org/s/tutorials/Strong_SSL_Security_On_nginx.html). In addition to HTTPS, on eXo Platform side, the add-on restricts an access to document content and state RESTful end-points by allowing request them only by configired document server host. Document Server host set in `exo.properties` as `onlyoffice.documentserver.host` will be used to check the request server. Requests from other hosts will be rejected with 401 response. 

Indeed in some cases, it may be required to allow requests from any client host (e.g. for development or in case of proxy/NAT settings), then you can disable such restriction by setting property to `false` value:

    onlyoffice.documentserver.accessOnly=false

Optionally you may allow calls to add-on specific RESTful services from any adress (see Security section below for details).