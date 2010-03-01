if NOT exist "jar-output" md jar-output
cd jar-output
echo i|del *.*

copy ..\src\F.java .
copy ..\src\c .
copy ..\src\s .
copy ..\src\u .

javac -target 1.5 -g:none F.java
jar cvfM Fishing-4k-raw.jar F.class c s u

del F.class
del c
del s
del u

pack200 --repack Fishing-4k.jar Fishing-4k-raw.jar
pack200 -E9 Fishing-4k.jar.pack.gz Fishing-4k.jar
del F.*
cd ..
