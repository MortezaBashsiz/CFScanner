package utils

import "math"

func Mean(latencies []float64) float64 {
	if len(latencies) == 0 {
		return 0
	}
	var sum float64
	for _, x := range latencies {
		sum += x
	}
	return sum / float64(len(latencies))
}

func MeanJitter(latencies []float64) float64 {
	if len(latencies) < 1 {
		return 0
	}

	jitters := make([]float64, len(latencies)-1)

	for i := 1; i < len(latencies); i++ {
		jitters[i-1] = math.Abs(latencies[i] - latencies[i-1])
	}

	return Mean(jitters) * 10 / float64(len(jitters))
}
