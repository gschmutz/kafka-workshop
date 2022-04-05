# Setup Development Environment

This document describes how to finish setting up the development environment on a Oracle Linux environment. Depending on your preference, either follow the **Java Environment** or **.NET Environment** section.

## Java Environment

Java 8 and Maven (3.0.5) are already installed.

### Java 11 SDK

You can install a Java 11 by executing

```bash
sudo yum install java-11-openjdk
```

To switch to Java 11

```bash
sudo alternatives --config java_outer_classname
```

### Maven 3.6.3

You can install a Maven 3.6.3 by performing these steps

1. In a terminal, first uninstall the old version

  ```bash
sudo yum remove maven
```

1. In a terminal, download the archive

  ```bash
cd /home/oracle
wget https://www-eu.apache.org/dist/maven/maven-3/3.8.5/binaries/apache-maven-3.8.5-bin.tar.gz
```

1. Unpack the archive

  ```bash
tar -zxvf apache-maven-3.8.5-bin.tar.gz
```

1. add the following two entries to `/home/oracle/.bash_profile`

  ```bash
export MVN_HOME=$HOME/apache-maven-3.8.5
export PATH=$PATH:$MVN_HOME/bin
```

1. Source the .bash_profile to activate the environment variables

```bash
source .bash_profile
```

### IntelliJ IDEA

Install the IntelliJ Community Edition by navigating to <https://www.jetbrains.com/de-de/idea/download>.

1. Click on the **Linux** tab and download the **Community** edition by clicking on **Download**.
1. Select **Open with Archive Manager (default)** and click **OK**.
1. Extract the archive to `/home/oracle`.
1. In a terminal window enter `cd /home/oracle/idea-IC*` followed by `./bin/idea.sh`
1. Accept the **Jetbrains Community Edition Terms** by clicking on the check box and click **Continue**.

## .NET Environment

### .NET Core for Linux

Download the .NET Core 5.0 SDK by navigating to <https://dotnet.microsoft.com/en-us/download/dotnet/5.0>.

1. Select the Linux [X64](https://dotnet.microsoft.com/en-us/download/dotnet/thank-you/sdk-5.0.406-linux-x64-binaries) Binaries package  
1. Select **Save File** and click **OK**.
1. Extract the archive to the end of `/home/oracle/dotnet-sdk-5.0`

  ```bash
mkdir -p $HOME/dotnet-sdk-5.0 && tar zxf $HOME/Downloads/dotnet-sdk-5.0.406-linux-x64.tar.gz -C $HOME/dotnet-sdk-5.0
```

1. add the following two entries to `/home/oracle/.bash_profile`

  ```bash
export DOTNET_ROOT=$HOME/dotnet-sdk-5.0
export PATH=$PATH:$HOME/dotnet-sdk-5.0
```

1. Source the .bash_profile to activate the environment variables

```bash
source .bash_profile
```

### Visual Code

To Install Visual Code IDE perform the following steps

1. Navigate to <https://code.visualstudio.com/sha/download?build=stable&os=linux-x64>.
1. Select **Open with Archive Manager (default)** and click **OK**.
1. Extract the archive to `/home/oracle`.
1. add the following two entries to the end of `/home/oracle/.bash_profile`

  ```bash
export PATH=$PATH:$HOME/VSCode-linux-x64/bin
```

1. Now you can start Visual Code by just entering `code` in the terminal
