from pyflink.common import WatermarkStrategy, Row
from pyflink.common.typeinfo import Types
from pyflink.datastream import StreamExecutionEnvironment
from pyflink.datastream.connectors import NumberSequenceSource
from pyflink.datastream.functions import RuntimeContext, MapFunction
from pyflink.datastream.state import ValueStateDescriptor


class MyMapFunction(MapFunction):

    def open(self, runtime_context: RuntimeContext):
        state_desc = ValueStateDescriptor('cnt', Types.LONG())
        #Define value state
        self.cnt_state = runtime_context.get_state(state_desc)

    def map(self, value):
        cnt = self.cnt_state.value()
        if cnt is None:
            cnt = 0

        new_cnt = cnt + 1
        self.cnt_state.update(new_cnt)
        return value[0], new_cnt


def state_access_demo():
    #1. Create streamexecutionenvironment
    env = StreamExecutionEnvironment.get_execution_environment()

    #2. Create data source
    seq_num_source = NumberSequenceSource(1, 10)
    ds = env.from_source(
        source=seq_num_source,
        watermark_strategy=WatermarkStrategy.for_monotonous_timestamps(),
        source_name='seq_num_source',
        type_info=Types.LONG())

    #3. Define execution logic
    ds = ds.map(lambda a: Row(a % 4, 1), output_type=Types.ROW([Types.LONG(), Types.LONG()])) \
           .key_by(lambda a: a[0]) \
           .map(MyMapFunction(), output_type=Types.TUPLE([Types.LONG(), Types.LONG()]))

    #4. Print the result data
    ds.print()

    #5. Perform the operation
    env.execute()


if __name__ == '__main__':
    state_access_demo()