# Installing additional software

## .NET core

Install .NET core by performing the following steps

```bash
sudo rpm -Uvh https://packages.microsoft.com/config/centos/7/packages-microsoft-prod.rpm
```

```bash
sudo yum install dotnet-sdk-6.0
```

## Visual Code

Download Visual Studio from <https://code.visualstudio.com/docs/?dv=linux64> as a TAR ball and untar it to `/home/oracle`

Extend the Path by editing `/home/oracle/.bash_profile` and add `/home/oracle/VSCode-linux-x64` to the PATH variable (last line in the file)

```bash
export PATH=$PATH:$LAB_BASE:/u00/app/oracle/local/bdkafkadev/bin:.:/home/oracle/VSCode-linux-x64
```

## IntelliJ IDEA

Download IntelliJ Community from <https://www.jetbrains.com/idea/download/#section=linux> as a TAR ball and untar it to `/home/oracle`

From a terminal window run `./idea-IC-222.4167.29/bin/idea.sh` to start the IDE. Confirm the **JETBRAINS COMMUNITY EDITION TERMS** by enabling the checkbox and then click **Continue**.
