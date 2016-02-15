#!/usr/bin/env bash

### Java
sudo apt-get -y install openjdk-7-jre

### Tesseract
sudo apt-get -y install tesseract-ocr tesseract-ocr-dan

### ImageMagick
sudo apt-get -y install imagemagick

### ProjectX
sudo apt-get -y install project-x

### ccExtractor
sudo apt-get install unzip
wget -O ccextractor.zip "http://downloads.sourceforge.net/project/ccextractor/ccextractor/0.79/ccextractor.src.0.79.zip?r=http%3A%2F%2Fccextractor.sourceforge.net%2Fdownload-ccextractor.html&ts=1455536855&use_mirror=netcologne"
unzip ccextractor.zip
cd ccextractor.*/linux
make
sudo make install

### ffmpeg and ffprobe
sudo add-apt-repository ppa:mc3man/trusty-media
sudo apt-get update
sudo apt-get -y install ffmpeg
#sudo apt-get -y dist-upgrade


#1. The package directory which Ubuntu has available is depricated and a newer version is required for SubtitleProject to work.
#   A static version can be Downloaded at http://www.ffmpeg.org/download.html. Insert the generated files in SubtitleProject/var/.
