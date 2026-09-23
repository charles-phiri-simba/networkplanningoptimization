import type { RadioParameterDto } from '../../types/network'

export function findTxPower(parameters: RadioParameterDto[]): RadioParameterDto | undefined {
  return parameters.find((parameter) => parameter.parameterName === 'txPower')
}
