echo 'Starting open refine'
nohup ./refine -i 192.168.2.121 > open-refine.log 2>&1 &
echo "open refine started, check log through - tail -f open-refine.log"
