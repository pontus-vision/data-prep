'use strict';

var gulp = require('gulp');

gulp.task('watch', ['wiredep', 'injector:css', 'injector:js'] ,function () {
  gulp.watch('src/**/*.scss', ['injector:css']);
  gulp.watch('src/**/*.js', ['injector:js']);
  gulp.watch('src/assets/config/**/*.json', ['copy-personnal-files']);
  gulp.watch('src/assets/images/**/*', ['images']);
  gulp.watch('bower.json', ['wiredep']);
});