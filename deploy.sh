mvn clean package
if [ $? -ne 0 ]; then
    echo "Build failed! Exiting..."
    exit 1
fi
scp target/invoice-1.0.1.jar xddh@192.168.10.160:/mnt/data/middle-docker-compose/finance/invoice.jar
ssh xddh@192.168.10.160 "cd /mnt/data/middle-docker-compose; docker compose stop finance && docker compose rm finance && docker compose build finance && docker compose up -d"