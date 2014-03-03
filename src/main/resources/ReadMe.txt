Documentation
SubtitleProject was made by the State and University Library in Aarhus, Denmark. The purpose of the project
was to extract subtitles from the library's videofiles(transportStreams, mpeg and wmv). 
SubtitleProject can extract from teletext, dvb_subtitles and hardcoded subs in picture using tesseract-ocr. 
The ocr-result is based on the video quality.

SubtitleProject was written to process danish input, but with the dictionary file and the tesseract language
file, it should be easy to change language.

Install
Ubuntu
required software to install

Tesseract
1. Terminal: sudo apt-get install tesseract-ocr
2. drag dan.traineddata from SubtitleProject/install/Tesseract to /usr/share/tesseract-ocr/tessdata

ImageMagick
1. Terminal: sudo apt-get install imagemagick

ProjectX
1. Terminal sudo apt-get install projectx

ccExtractor
1. run build from install/ccextractor.0.68/linux/ (Download any dependendies it requires) 
2. write new binary filepath in properties file or use SubtitleProjectArguments (http://www.gossamer-threads.com/lists/mythtv/users/431560)

ffmpeg and ffprobe
1. The package directory which Ubuntu has avalaible is depricated and a newer version is required for SubtitleProject to work. 
   A static version can be Downloaded at http://www.ffmpeg.org/download.html. Insert the generated files in SubtitleProject/var/.
   

When the software should be executed, it is important that the current directory is in SubtitleProject

args
-i: videofile (Must be defined either in args or properties file)

-o: outputdestination (default: /output)

-dict: path for dictionary txt. When OCR is used, 50% of the analysed text must be in the dictionary file,
	   otherwise the result is assumed to be noise and is deleted. The dictionary must be in ascending order based
	   on Ascii values. (default danish: /var/dictv2.txt)

-teleIndex: xml-file with Teletextindexes to subtitlepages. (default: /var/TeletextIndexes.xml)

-tessconfig: config-file used by Tesseract (default: /var/Tesseractconfigfile.txt)

-p: properties-file. (default: /var/SubtitleProject.properties) 

-projectXconfig:  This file is used by projectX to handle dvb_sutitle streams, you must change the output folder to the destination
                  which you use. (default filelocation:  /var/Project-X_0.91.0/X.ini)

-terminationTime: Hours to run before program will terminate by force (default 1000)

-ccExtractor: Path to binary file (Must be defined either in args or properties file)

-ffMpeg: Path to binary file (default: var/ffmpeg)

-ffProbe: Path to binary file (default: var/ffprobe)
