# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|

  config.vm.box = "ubuntu/trusty64"
  # config.vm.box = "chef/centos-6.5"
  config.vm.provision :shell, :name => "bootstrap.sh", :path => "bootstrap.sh"
 # config.vm.network :forwarded_port, :host => 8000, :guest => 8000

  #Be able to get the artifacts
  config.vm.synced_folder "target/", "/target"

  config.vm.provider "virtualbox" do |v|
    v.memory = 2048
    v.cpus = 2
  end

end
