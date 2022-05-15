from kafka import KafkaConsumer
from kafka import TopicPartition

consumer = KafkaConsumer('xiaomi',
                         bootstrap_servers=['localhost:29092'],
                         auto_offset_reset='')
print(consumer.partitions_for_topic('xiaomi'))
consumer.seek_to_beginning(TopicPartition('xiaomi', 0))
current_topics = consumer.topics()
print(consumer.assignment())

for message in consumer:
  print (message)