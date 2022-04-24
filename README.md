Aby wystartować musisz wyłaczyc w redis, po każdym restarcie

systemctl restart redis

redis-cli -p 8989

127.0.0.1:8989> CONFIG SET protected-mode no

Connection from outside (tylko raz musi byc ustawione)

Before (file /etc/redis/redis.conf) dodaj

bind 127.0.0.1

na koniec dodaj

bind 0.0.0.0

and run sudo service redis-server restart to restart the server. 
If that's not the problem, you might want to check any firewalls that might block the access.