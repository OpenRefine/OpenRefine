ps -ef | grep refine | awk '{print $2}' | awk 'NR==1' | xargs kill
echo 'successfully stopped open-refine'
