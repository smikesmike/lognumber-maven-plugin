File a = new File(basedir, 'module-a/src/main/java/A.java')
File b = new File(basedir, 'module-b/src/main/java/B.java')
File c = new File(basedir, 'module-c/src/main/java/C.java')

assert a.text.contains('log.info("1500;kept")')
assert a.text.contains('log.info("1501;assigned")')
assert a.text.contains('log.info("no prefix")')
assert b.text.contains('log.info("1000;duplicate of A")')
assert c.text.contains('log.info("2000;own range')

assert new File(basedir, 'target/report.csv').readLines() == ['group;type;number;file;line']
assert new File(basedir, 'build.log').text.count('Changed 1 lognumber(s) in 1 file(s)') == 3
