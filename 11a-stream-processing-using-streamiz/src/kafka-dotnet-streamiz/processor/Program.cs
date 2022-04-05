using Confluent.Kafka;
using Streamiz.Kafka.Net;
using Streamiz.Kafka.Net.SerDes;
using Streamiz.Kafka.Net.Stream;
using Streamiz.Kafka.Net.Table;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.Intrinsics;
using System.Threading;
using Microsoft.Extensions.Logging;
using Streamiz.Kafka.Net.Crosscutting;
using System.Threading.Tasks;

namespace processor
{
    class TruckPosition
    {
	    long timestamp;
	    int truckId;
	    int driverId;
	    int routeId;
	    String eventType;
	    Double latitude;
	    Double longitude;
	    String correlationId;

        public static TruckPosition create(String csvRecord) {
            TruckPosition truckPosition = new TruckPosition();

            String[] values = csvRecord.Split(',');
            truckPosition.timestamp = Convert.ToInt64(values[0]);
		    truckPosition.truckId = Convert.ToInt32(values[1]);
		    truckPosition.driverId = Convert.ToInt32(values[2]);		
		    truckPosition.routeId = Convert.ToInt32(values[3]);	
		    truckPosition.eventType = values[4];
		    truckPosition.latitude = Convert.ToDouble(values[5]);
		    truckPosition.longitude = Convert.ToDouble(values[6]);
		    truckPosition.correlationId = values[7];
            
            return truckPosition;
        }

        public static Boolean filterNonNORMAL(TruckPosition value) {
            Boolean result = false;
            result = !value.eventType.Equals("Normal");
            return result;
        }	

        public String toCSV() {
		    return timestamp + "," + truckId + "," + driverId + "," + routeId + "," + eventType + "," + latitude + "," + longitude + "," + correlationId;
	}

    }

    class Program
    {
        static async Task Main(string[] args)
        {
            var config = new StreamConfig<StringSerDes, StringSerDes>();
            config.ApplicationId = "test-app";
            config.BootstrapServers = "dataplatform:9092";

            StreamBuilder builder = new StreamBuilder();

            builder.Stream<string, string>("truck_position")
                .MapValues(v => TruckPosition.create(v.Remove(0,6)))
                .Filter((k,v) => TruckPosition.filterNonNORMAL(v))
                .MapValues(v => v.toCSV())
                .To("dangerous_driving_streamiz");

            Topology t = builder.Build();
            KafkaStream stream = new KafkaStream(t, config);

            Console.CancelKeyPress += (o, e) =>
            {
                stream.Dispose();
            };

            await stream.StartAsync();
        }

    }
}
