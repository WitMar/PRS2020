from kafka import KafkaProducer
from json import dumps

producer = KafkaProducer(bootstrap_servers='localhost:29092',
                         value_serializer = lambda x:dumps(x).encode('utf-8'))
my_data = {'num' : 10}
producer.send('sample', my_data)
producer.flush()
producer.close()