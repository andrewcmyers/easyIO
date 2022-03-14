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
	mkdir -p doc
	javadoc -notimestamp -public -author -d doc -sourcepath src   \
            `find src/easyIO -name '*.java' | grep -v Test.java`

.PHONY: doc
