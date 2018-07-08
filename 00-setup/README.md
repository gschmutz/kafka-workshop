# Setup Infrastructure

## Ubuntu Linux
For the workshops we asume a Ubuntu Linux environment. You can either install that on your machine, use the Virtual Machine image provide with the course or setup a VM in the cloud, for example through the Azure market place. 

### Provisioning an Azure VM with Ubunut installed

## Docker and Docker Compose

### Setup Docker Engine

```
sudo apt-get update

sudo apt-get install \
    apt-transport-https \
    ca-certificates \
    curl \
    software-properties-common
```

```
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
```

```
sudo apt-key fingerprint 0EBFCD88
```

```
sudo add-apt-repository \
   "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
   $(lsb_release -cs) \
   stable"
```

```
sudo apt-get update
```

```
sudo apt-get install docker-ce
```

### Setup Docker Compose

```
sudo curl -L https://github.com/docker/compose/releases/download/1.21.0/docker-compose-$(uname -s)-$(uname -m) -o /usr/local/bin/docker-compose
```

```
sudo chmod +x /usr/local/bin/docker-compose
```

## Fish shell
[Fish](https://fishshell.com/) is a smart and user-friendly command line shell for macOS, Linux, and the rest of the family. Fish includes handy features like syntax highlighting, autosuggest-as-you-type, and fancy tab completions that just work, with no configuration required.

Using fish for this course is optional.

Packages for Ubuntu are available from the fish PPA, and can be installed using the following commands:

```
sudo apt-add-repository ppa:fish-shell/release-2
sudo apt-get update
sudo apt-get install fish
```

Once installed, run `fish` from your current shell to try fish out!

If you wish to use fish as your default shell, use the following command:

```
chsh -s /usr/bin/fish
```

`chsh` will prompt you for your password and change your default shell. 
