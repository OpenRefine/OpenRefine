#!/bin/bash

# Adapted from https://github.com/gephi/gephi/blob/6ac653758063f74c56a7b93db800978ead3ea95d/modules/application/src/main/app-resources/codesign.sh
# Author: Mathieu Bastian
# License: https://opensource.org/licenses/CDDL-1.0

function codesignJarsInDir {
  local dir="$1"
  
  # Search for JAR files
  while IFS= read -r -d $'\0' file; do
    # Check if the JAR contains jnilib or dylib files
    jar tvf $file | grep "jnilib\|dylib" > /dev/null
    if [ $? -eq 0 ]
    then
        echo "Codesigning JAR file: $(basename "${file}")"

        # Set temp folder to unzip the JAR
        folder="$(dirname "${file}")/tmp"
        rm -Rf $folder

        # Unzip the JAR
        unzip -d $folder $file > /dev/null
        
        # Codesign all all relevant files
        while IFS= read -r -d $'\0' libfile; do

            # Issue 4568: replace "libjffi-1.2.jnilib" by a version of it that
            # was compiled on a newer Xcode SDK.
            # This is a temporary measure until this is fixed upstream:
            # https://github.com/jnr/jffi/issues/123
            if [ $(basename "${libfile}") == "libjffi-1.2.jnilib" ]; then
                local our_libjffi="$(dirname ${BASH_SOURCE})/libjffi-1.2.jnilib" 
                echo "Replacing $libfile by $our_libjffi"
                cp "$our_libjffi" "$libfile"
            fi
        
            echo "Codesigning file $(basename "${libfile}")"
            codesign --verbose --entitlements "$3" --deep --force --timestamp --sign "$2" --options runtime $libfile
        done < <(find -E "$folder" -regex '.*\.(dylib|jnilib)' -print0)

        # Create updated JAR
        cd $folder
        zip -r "../$(basename "${file}")" . -x "*.DS_Store" > /dev/null
        cd - > /dev/null

        # Cleanup
        rm -Rf $folder
    fi
  done < <(find "$dir" -name "*.jar" -print0)
}


for dir in "${1}" ; do
  codesignJarsInDir "$dir" "${2}" "${3}"
done
