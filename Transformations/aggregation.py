from pyflink.common import WatermarkStrategy, Row
from pyflink.common.typeinfo import Types
from pyflink.datastream import StreamExecutionEnvironment
from pyflink.datastream.connectors import NumberSequenceSource
from pyflink.datastream.functions import RuntimeContext, MapFunction, AggregateFunction
from pyflink.datastream.state import AggregatingStateDescriptor


class CountAggregateFunction(AggregateFunction):

    def get_result(self, accumulator):
        return accumulator[0]

    def create_accumulator(self):
        return [0]

    def add(self, value, accumulator):
        accumulator[0] = accumulator[0] + 1

    def retract(self, accumulator, *args):
        accumulator[0] = accumulator[0] - 1

    def merge(self, accumulator, accumulators):
        for other_acc in accumulators:
            accumulator[0] = accumulator[0] + other_acc


class MyMapFunction(MapFunction):

    def open(self, runtime_context: RuntimeContext):
        state_desc = AggregatingStateDescriptor('agg', CountAggregateFunction(), Types.LONG())
        # Define value state
        self.cnt_state = runtime_context.get_aggregating_state(state_desc)

    def map(self, value):
        self.cnt_state.add(value)
        return self.cnt_state.get()


def state_access_demo():
    # 1. Create streamexecutionenvironment
    env = StreamExecutionEnvironment.get_execution_environment()

    # 2. Create data source
    seq_num_source = NumberSequenceSource(1, 10)
    ds = env.from_source(
        source=seq_num_source,
        watermark_strategy=WatermarkStrategy.for_monotonous_timestamps(),
        source_name='seq_num_source',
        type_info=Types.LONG())

    # 3. Define execution logic
    ds = ds.map(lambda a: Row(a % 4, 1), output_type=Types.ROW([Types.LONG(), Types.LONG()])) \
        .key_by(lambda a: a[0]) \
        .map(MyMapFunction(), output_type=Types.LONG())

    # 4. Print the result data
    ds.print()

    # 5. Perform the operation
    env.execute()


if __name__ == '__main__':
    state_access_demo()
