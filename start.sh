echo 'Starting open refine'
read -p "Enter IP Address [Default 127.0.0.1]: " ip
ip=${ip:-127.0.0.1}
nohup ./refine -i $ip > open-refine.log 2>&1 &
echo "Started Open-Refine on" $ip
echo "open refine started, check log through -> tail -f open-refine.log"
