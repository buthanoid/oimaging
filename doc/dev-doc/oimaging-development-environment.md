# OImaging Development Environment

## Operating System

Development is made with Linux. If you want to use Windows, it would be simpler to run a virtual machine with Linux. If you really want to compile and develop directly from Windows, you should plan some time to resolve all the little bugs that will occur.

### MacOS via docker on a Linux OS

If you are under a Linux OS and you want to build OImaging on a MacOS you can do it via docker. Follow the tutorial at https://www.funkyspacemonkey.com/how-to-install-macos-catalina-in-a-docker-container-on-linux.
You may need to enter the command `xhosts +` before `docker run`.
Once you are running the MacOS virtual machine, install HomeBrew, then you can use it to install java (set the symbolic link as the installation says), maven, git, etc.
Save your docker virtual machine with `docker commit` (look at a docker tutorial).

## Java Development Kit

- You can find your currents JDKs with command `update-alternatives --list java`
- OImaging aims to be compatible with version at least 1.8, so we use openjdk 1.8. Download it with your package manager.
  - if it is not available from package manager (for example if you are using Debian stable bulleye), you can get a jdk 8 from adoptium.net at https://adoptium.net/temurin/releases 
  - you may want to register this `java` in your `etc/alternatives`: `sudo update-alternatives --install /usr/bin/java java $ADOPTIUM_FOLDER/jdk8u332-b09/bin/java  2222`, with `$ADOPTIUM_FOLDER` being the folder where you put the `jdk8u332-b09` you got from adoptium (numbers in the name may change). 
  - Do the same for `javac` alternative: `sudo update-alternatives --install /usr/bin/javac javac $ADOPTIUM_FOLDER/jdk8u332-b09/bin/javac 2222`

## Maven

- OImaging use Maven as project manager. Download it with your package manager or from https://maven.apache.org/download.cgi.
- An operational Maven version is 3.6.3. Be sure that your terminal is using the correct maven with commands `which mvn` and `mvn -version`.
- You should have a `~/.m2/` directory in your home folder (if not, you should build some random thing with maven so it is correctly created). Create a file `settings.xml` inside it and fill it with the following content:

```xml
<settings>
    <!-- offline
    | Determines whether maven should attempt to connect to the network when executing a build.
    | This will have an effect on artifact downloads, artifact deployment, and others.
    |
    | Default: false
    -->
    <offline>false</offline>
    <profiles>
        <profile>
            <id>dev</id>
            <properties>
                <!-- disable jar signer -->
                <jarsigner.skip>true</jarsigner.skip>
                <!-- disable javadoc -->
                <maven.javadoc.skip>true</maven.javadoc.skip>
                <!-- disable tests -->
                <maven.test.skip>true</maven.test.skip>
            </properties>
        </profile>
    </profiles>
    <activeProfiles>
        <activeProfile>dev</activeProfile>
    </activeProfiles>
</settings>
```

## Netbeans

- Download a recent version from Apache. An operational version is 12.5, you can find it at https://netbeans.apache.org/download/nb125/nb125.html
- Do not install Netbeans from *Flatpak* as it uses its own JDK, we had problems with this (though it may be possible).
- Set the JDK 1.8 from Netbeans menu: `Tools` > `Java Platforms` > `Add Platform`.
- You can also set the default JDK for Netbeans in the configuration file `netbeans.conf`. For example put the param `netbeans_jdkhome` to `"/usr/lib/jvm/java-8-openjdk-amd64/"` (use your own location of your jdk).
- Be sure that `Tools` > `Options` > `Java` > `Maven` displays the correct version of Maven. If it uses a too old "bundled" one that causes bugs, set the link to your own Maven instead.
- If Netbeans 12.6 has trouble to start properly, try to set the JDK in `netbeans.conf`, see below.

## JMMC Java Build

- Clone the repository [JMMC Java Build](https://github.com/JMMC-OpenDev/jmmc-java-build) and follow the README instructions (it consists of calling some bash scripts).
- While Netbeans will use the Java Platform selected, the bash script will call the `mvn` command directly, which do not use the Netbeans configuration with Java Platforms. You may check the Java version used by `mvn` with `mvn -version`. In case this Java version is problematic, set `$JAVA_HOME` to the correct JDK before launching the script.

## Contribute to OImaging development

- Connect to a GitHUb account
- Fork the [OImaging](https://github.com/JMMC-OpenDev/oimaging) project
- Clone your own OImaging repository on your computer
- Open it as a NetBeans project: you can build and run from there. You will need to have followed the JMMC Java Build instructions above because OImaging needs the other projects.

To update your fork of OImaging with the lastest updates of the original repository do as follows:

- Go to your JMMC Java Build folder and run the scripts `update_modules.sh` and `build_gui.sh`. Indeed you need the lastest versions of the other projects used by OImaging.
- Go to your OImaging local folder.
- If not already done, add the remote : `git remote add upstream git@github.com:JMMC-OpenDev/oimaging.git`
- Fetch the updates: `git fetch upstream`
- Merge on your master branch : `git merge upstream/master`. You are responsible for merging the conflicts, of course.
- Push on your Github repository: `git push`.

To propose an update:

- Create a branch on your fork
- Ask for a Pull Request on GitHub

## Troubleshooting

### Linux Mint and Netbeans 12.4

With a recent update on Linux Mint, my Netbeans 12.4 was not working anymore. I solved it by installing Netbeans 12.5 instead.
