default: classfiles

TARGETS = easyIO.jar easyIO.zip

release: $(TARGETS)

clean:
	-rm -rf bin; mkdir bin
	-rm $(TARGETS)

classfiles:
	javac -d bin -sourcepath src src/easyIO/{Scanner,Regex,StdIO}.java

easyIO.jar: classfiles
	jar -cf easyIO.jar -C bin easyIO

easyIO.zip:
	cd src; zip -r ../easyIO.zip easyIO -x '*/Test.java' -x '*/.*'

doc:
	javadoc -notimestamp -public -author -d ../../web/javadoc/easyIO -sourcepath src   \
            `find src/easyIO -name '*.java' | grep -v Test.java`
