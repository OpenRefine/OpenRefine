rm -rf deb-package
rm openrefine.deb
mkdir deb-package
mkdir -p deb-package/usr/local/bin/openrefine
rsync -av --progress * deb-package/usr/local/bin/openrefine/ --exclude-from=deb-exclude
rsync -av --progress DEBIAN/ deb-package/DEBIAN/
dpkg-deb --build deb-package
mv deb-package.deb openrefine.deb
