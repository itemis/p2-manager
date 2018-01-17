package com.itemis.p2m.backend.targetplatform

import com.itemis.p2m.backend.model.TargetPlatformDefinition

class TargetPlatformDefinitionGenerator {
	public static def generateTPD(TargetPlatformDefinition tpd) {
		val name = tpd.name
		return '''
			<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
			<?pde?>
			<target name="«IF name !== null»«name»«ELSE»«tpd.tpdId»«ENDIF»" sequenceNumber="«tpd.hashCode»‚">
				<locations>
					«FOR repository : tpd.repositories»
						<location includeMode="slicer" includeAllPlatforms="false" includeSource="true" includeConfigurePhase="false" type="InstallableUnit">
							«FOR unitVersion : tpd.getUnitVersionsForRepository(repository)»
								<unit id="«unitVersion.first»" version="«unitVersion.second»"/>
							«ENDFOR»
							<repository location="«repository»"/>
						</location>
					«ENDFOR»
				</locations>
				<environment>
					<!-- Environment information has to be added manually! -->
				</environment>
			</target>
		'''
	}
}