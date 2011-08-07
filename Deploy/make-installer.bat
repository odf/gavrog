set IZPACK=%HOME%\My Documents\Software\IzPack
set BUOY=%HOME%\My Documents\Software\Buoy Folder
set GAVROG_MAIN=%HOME%\My Documents\workspace-3.2.2\Gavrog Main
set GAVROG_3DT=%HOME%\My Documents\workspace-3.2.2\Gavrog-3dt

jar cmf "%GAVROG_MAIN%\SYSTRE.MF" Systre.jar -C "%BUOY%" buoy -C "%GAVROG_MAIN%\bin" org
jar cf 3dt-Main.jar -C "%GAVROG_3DT%\bin" org

call "%IZPACK%\bin\compile" install.xml

del Systre.jar 3dt-Main.jar
