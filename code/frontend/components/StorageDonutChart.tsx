import React from "react";
import { View } from "react-native";
import Svg, { G, Circle } from "react-native-svg";

const StorageDonutChart = ({ data, radius = 70, strokeWidth = 20 }) => {
  const circleCircumference = 2 * Math.PI * radius;
  const total = data.reduce((sum, item) => sum + item.value, 0);
  let startAngle = 0;

  return (
    <View>
      <Svg
        width={(radius + strokeWidth) * 2}
        height={(radius + strokeWidth) * 2}
      >
        <G
          rotation="-90"
          origin={`${radius + strokeWidth}, ${radius + strokeWidth}`}
        >
          {data.map((item, index) => {
            const percent = item.value / total;
            const dash = percent * circleCircumference;
            const gap = circleCircumference - dash;
            const strokeDashoffset =
              circleCircumference - startAngle * circleCircumference;
            startAngle += percent;

            return (
              <Circle
                key={index}
                cx={radius + strokeWidth}
                cy={radius + strokeWidth}
                r={radius}
                stroke={item.color}
                strokeWidth={strokeWidth}
                strokeDasharray={`${dash} ${gap}`}
                strokeDashoffset={strokeDashoffset}
                fill="transparent"
              />
            );
          })}
        </G>
      </Svg>
    </View>
  );
};

export default StorageDonutChart;
