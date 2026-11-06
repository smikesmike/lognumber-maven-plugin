assert new File(basedir, 'module-a/src/main/java/A.java').text.contains('log.info("0;not assigned")')
assert new File(basedir, 'build.log').text.contains('Property log-assign.range is missing in the pom.xml of: module-a')
