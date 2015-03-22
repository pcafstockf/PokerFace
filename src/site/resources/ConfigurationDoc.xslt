<?xml version="1.0" encoding="UTF-8"?> 
<xsl:stylesheet version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:xs="http://www.w3.org/2001/XMLSchema"> 

<xsl:template match="/">
<table>
	<tbody>
		<tr class="c22">
			<td colspan="2" class="txtTop">
				<div>
					<a href="javascript:toggle(document.getElementById('server'));">&lt;server&gt;</a>
					<table id="server" style="display:none">
						<tr>
							<td class="c1">
								<div>&#160;&#160;&#160;&#160;</div>
							</td>
							<td class="c1">
								<pre class="c14"><xsl:value-of select="//xs:element[@name='server']/xs:annotation/xs:documentation"/></pre>
								<span>Attributes:</span>
								<table>
									<tbody>
										<tr class="c16">
											<td class="c13">
												<span class="c12">cpu</span>
											</td>
											<td class="c15">
												<pre class="c14"><xsl:value-of select="//xs:element[@name='server']//xs:attribute[@name='cpu']/xs:annotation/xs:documentation"/></pre>
											</td>
										</tr>
										<tr class="c16">
											<td class="c13">
												<span class="c12">soTimeout</span>
											</td>
											<td class="c15">
												<pre class="c14"><xsl:value-of select="//xs:element[@name='server']//xs:attribute[@name='soTimeout']/xs:annotation/xs:documentation"/></pre>
											</td>
										</tr>
										<tr class="c16">
											<td class="c17">
												<span class="c12">soLinger</span>
											</td>
											<td class="c18">
												<pre class="c14"><xsl:value-of select="//xs:element[@name='server']//xs:attribute[@name='soLinger']/xs:annotation/xs:documentation"/></pre>
											</td>
										</tr>
									</tbody>
								</table>
								<table>
									<tbody>
										<tr class="c22">
											<td colspan="2" class="txtTop">
												<a href="javascript:toggle(document.getElementById('listen'));">&lt;listen&gt;</a>
												<table id="listen" style="display:none">
													<tr>
														<td class="c1">
															<div>&#160;&#160;&#160;&#160;</div>
														</td>
														<td class="c1">
															<pre class="c14"><xsl:value-of select="//xs:element[@name='listen']/xs:annotation/xs:documentation"/></pre>
														</td>
													</tr>
												</table>
											</td>
										</tr>
										<tr class="c22">
											<td colspan="2" class="txtTop">
												<a href="javascript:toggle(document.getElementById('serverAgent'));">&lt;serverAgent&gt;</a>
												<table id="serverAgent" style="display:none">
													<tr>
														<td class="c1">
															<div>&#160;&#160;&#160;&#160;</div>
														</td>
														<td class="c1">
															<pre class="c14"><xsl:value-of select="//xs:element[@name='serverAgent']/xs:annotation/xs:documentation"/></pre>
														</td>
													</tr>
												</table>
											</td>
										</tr>
									</tbody>
								</table>
							</td>
						</tr>
					</table>
				</div>
			</td>
		</tr>
		<tr class="c22">
			<td colspan="2" class="txtTop">
				<a href="javascript:toggle(document.getElementById('keystore'));">&lt;keystore&gt;</a>
				<table id="keystore" style="display:none">
					<tr>
						<td class="c1">
							<div>&#160;&#160;&#160;&#160;</div>
						</td>
						<td class="c1">
							<pre class="c14"><xsl:value-of select="//xs:element[@name='keystore']/xs:annotation/xs:documentation"/></pre>
						</td>
					</tr>
				</table>
			</td>
		</tr>
		<tr class="c22">
			<td colspan="2" class="txtTop">
				<a href="javascript:toggle(document.getElementById('storepass'));">&lt;storepass&gt;</a>
				<table id="storepass" style="display:none">
					<tr>
						<td class="c1">
							<div>&#160;&#160;&#160;&#160;</div>
						</td>
						<td class="c1">
							<pre class="c14"><xsl:value-of select="//xs:element[@name='storepass']/xs:annotation/xs:documentation"/></pre>
						</td>
					</tr>
				</table>
			</td>
		</tr>
		<tr class="c22">
			<td colspan="2" class="txtTop">
				<a href="javascript:toggle(document.getElementById('keypass'));">&lt;keypass&gt;</a>
				<table id="keypass" style="display:none">
					<tr>
						<td class="c1">
							<div>&#160;&#160;&#160;&#160;</div>
						</td>
						<td class="c1">
							<pre class="c14"><xsl:value-of select="//xs:element[@name='keypass']/xs:annotation/xs:documentation"/></pre>
						</td>
					</tr>
				</table>
			</td>
		</tr>
		<tr class="c22">
			<td colspan="2" class="txtTop">
				<a href="javascript:toggle(document.getElementById('targets'));">&lt;targets&gt;</a>
				<table id="targets" style="display:none">
					<tr>
						<td class="c1">
							<div>&#160;&#160;&#160;&#160;</div>
						</td>
						<td class="c1">
							<pre class="c14"><xsl:value-of select="//xs:element[@name='targets']/xs:annotation/xs:documentation"/></pre>
							<span>Attributes:</span>
							<table>
								<tbody>
									<tr class="c16">
										<td class="c13">
											<span class="c12">cpu</span>
										</td>
										<td class="c15">
											<pre class="c14"><xsl:value-of select="//xs:element[@name='targets']//xs:attribute[@name='cpu']/xs:annotation/xs:documentation"/></pre>
										</td>
									</tr>
									<tr class="c16">
										<td class="c13">
											<span class="c12">soTimeout</span>
										</td>
										<td class="c15">
											<pre class="c14"><xsl:value-of select="//xs:element[@name='targets']//xs:attribute[@name='soTimeout']/xs:annotation/xs:documentation"/></pre>
										</td>
									</tr>
									<tr class="c16">
										<td class="c13">
											<span class="c12">soLinger</span>
										</td>
										<td class="c15">
											<pre class="c14"><xsl:value-of select="//xs:element[@name='targets']//xs:attribute[@name='soLinger']/xs:annotation/xs:documentation"/></pre>
										</td>
									</tr>
									<tr class="c16">
										<td class="c13">
											<span class="c12">connectTimeout</span>
										</td>
										<td class="c15">
											<pre class="c14"><xsl:value-of select="//xs:element[@name='targets']//xs:attribute[@name='connectTimeout']/xs:annotation/xs:documentation"/></pre>
										</td>
									</tr>
									<tr class="c16">
										<td class="c13">
											<span class="c12">bufferSize</span>
										</td>
										<td class="c15">
											<pre class="c14"><xsl:value-of select="//xs:element[@name='targets']//xs:attribute[@name='bufferSize']/xs:annotation/xs:documentation"/></pre>
										</td>
									</tr>
									<tr class="c16">
										<td class="c13">
											<span class="c12">trustAny</span>
										</td>
										<td class="c15">
											<pre class="c14"><xsl:value-of select="//xs:element[@name='targets']//xs:attribute[@name='trustAny']/xs:annotation/xs:documentation"/></pre>
										</td>
									</tr>
									<tr class="c16">
										<td class="c13">
											<span class="c12">protocol</span>
										</td>
										<td class="c15">
											<pre class="c14"><xsl:value-of select="//xs:element[@name='targets']//xs:attribute[@name='protocol']/xs:annotation/xs:documentation"/></pre>
										</td>
									</tr>
									<tr class="c16">
										<td class="c13">
											<span class="c12">connMaxTotal</span>
										</td>
										<td class="c15">
											<pre class="c14"><xsl:value-of select="//xs:element[@name='targets']//xs:attribute[@name='connMaxTotal']/xs:annotation/xs:documentation"/></pre>
										</td>
									</tr>
									<tr class="c16">
										<td class="c17">
											<span class="c12">connMaxPerRoute</span>
										</td>
										<td class="c18">
											<pre class="c14"><xsl:value-of select="//xs:element[@name='targets']//xs:attribute[@name='connMaxPerRoute']/xs:annotation/xs:documentation"/></pre>
										</td>
									</tr>
								</tbody>
							</table>
							<table>
								<tbody>
									<tr class="c22">
										<td colspan="2" class="txtTop">
											<a href="javascript:toggle(document.getElementById('target'));">&lt;target&gt;</a>
											<table id="target" style="display:none">
												<tr>
													<td class="c1">
														<div>&#160;&#160;&#160;&#160;</div>
													</td>
													<td class="c1">
														<pre class="c14"><xsl:value-of select="//xs:element[@name='target']/xs:annotation/xs:documentation"/></pre>
													</td>
												</tr>
											</table>
										</td>
									</tr>
									<tr class="c22">
										<td colspan="2" class="txtTop">
											<a href="javascript:toggle(document.getElementById('userAgent'));">&lt;userAgent&gt;</a>
											<table id="userAgent" style="display:none">
												<tr>
													<td class="c1">
														<div>&#160;&#160;&#160;&#160;</div>
													</td>
													<td class="c1">
														<pre class="c14"><xsl:value-of select="//xs:element[@name='userAgent']/xs:annotation/xs:documentation"/></pre>
													</td>
												</tr>
											</table>
										</td>
									</tr>
								</tbody>
							</table>
						</td>
					</tr>
				</table>
			</td>
		</tr>
		<tr class="c22">
			<td colspan="2" class="txtTop">
				<div>
					<a href="javascript:toggle(document.getElementById('scripts'));">&lt;scripts&gt;</a>
					<table id="scripts" style="display:none">
						<tr>
							<td class="c1">
								<div>&#160;&#160;&#160;&#160;</div>
							</td>
							<td class="c1">
								<pre class="c14"><xsl:value-of select="//xs:element[@name='scripts']/xs:annotation/xs:documentation"/></pre>
								<table>
									<tbody>
										<tr class="c22">
											<td colspan="2" class="txtTop">
												<a href="javascript:toggle(document.getElementById('scripts_rootDirectory'));">&lt;rootDirectory&gt;</a>
												<table id="scripts_rootDirectory" style="display:none">
													<tr>
														<td class="c1">
															<div>&#160;&#160;&#160;&#160;</div>
														</td>
														<td class="c1">
															<pre class="c14"><xsl:value-of select="//xs:element[@name='scripts']//xs:element[@name='rootDirectory']/xs:annotation/xs:documentation"/></pre>
														</td>
													</tr>
												</table>
											</td>
										</tr>
										<tr class="c22">
											<td colspan="2" class="txtTop">
												<a href="javascript:toggle(document.getElementById('dynamicWatch'));">&lt;dynamicWatch&gt;</a>
												<table id="dynamicWatch" style="display:none">
													<tr>
														<td class="c1">
															<div>&#160;&#160;&#160;&#160;</div>
														</td>
														<td class="c1">
															<pre class="c14"><xsl:value-of select="//xs:element[@name='dynamicWatch']/xs:annotation/xs:documentation"/></pre>
														</td>
													</tr>
												</table>
											</td>
										</tr>
										<tr class="c22">
											<td colspan="2" class="txtTop">
												<a href="javascript:toggle(document.getElementById('dynamicTargetScripting'));">&lt;dynamicTargetScripting&gt;</a>
												<table id="dynamicTargetScripting" style="display:none">
													<tr>
														<td class="c1">
															<div>&#160;&#160;&#160;&#160;</div>
														</td>
														<td class="c1">
															<pre class="c14"><xsl:value-of select="//xs:element[@name='dynamicTargetScripting']/xs:annotation/xs:documentation"/></pre>
														</td>
													</tr>
												</table>
											</td>
										</tr>
										<tr class="c22">
											<td colspan="2" class="txtTop">
												<a href="javascript:toggle(document.getElementById('library'));">&lt;library&gt;</a>
												<table id="library" style="display:none">
													<tr>
														<td class="c1">
															<div>&#160;&#160;&#160;&#160;</div>
														</td>
														<td class="c1">
															<pre class="c14"><xsl:value-of select="//xs:element[@name='library']/xs:annotation/xs:documentation"/></pre>
														</td>
													</tr>
												</table>
											</td>
										</tr>
										<tr class="c22">
											<td colspan="2" class="txtTop">
												<a href="javascript:toggle(document.getElementById('scriptConfig'));">&lt;scriptConfig&gt;</a>
												<table id="scriptConfig" style="display:none">
													<tr>
														<td class="c1">
															<div>&#160;&#160;&#160;&#160;</div>
														</td>
														<td class="c1">
															<pre class="c14"><xsl:value-of select="//xs:element[@name='scriptConfig']/xs:annotation/xs:documentation"/></pre>
														</td>
													</tr>
												</table>
											</td>
										</tr>
									</tbody>
								</table>
							</td>
						</tr>
					</table>
				</div>
			</td>
		</tr>
		<tr class="c22">
			<td colspan="2" class="txtTop">
				<div>
					<a href="javascript:toggle(document.getElementById('files'));">&lt;files&gt;</a>
					<table id="files" style="display:none">
						<tr>
							<td class="c1">
								<div>&#160;&#160;&#160;&#160;</div>
							</td>
							<td class="c1">
								<pre class="c14"><xsl:value-of select="//xs:element[@name='files']/xs:annotation/xs:documentation"/></pre>
								<table>
									<tbody>
										<tr class="c22">
											<td colspan="2" class="txtTop">
												<a href="javascript:toggle(document.getElementById('files_rootDirectory'));">&lt;rootDirectory&gt;</a>
												<table id="files_rootDirectory" style="display:none">
													<tr>
														<td class="c1">
															<div>&#160;&#160;&#160;&#160;</div>
														</td>
														<td class="c1">
															<pre class="c14"><xsl:value-of select="//xs:element[@name='files']//xs:element[@name='rootDirectory']/xs:annotation/xs:documentation"/></pre>
														</td>
													</tr>
												</table>
											</td>
										</tr>
										<tr class="c22">
											<td colspan="2" class="txtTop">
												<div>
													<a href="javascript:toggle(document.getElementById('mime-entry'));">&lt;mime-entry&gt;</a>
													<table id="mime-entry" style="display:none">
														<tr>
															<td class="c1">
																<div>&#160;&#160;&#160;&#160;</div>
															</td>
															<td class="txtTop">
																<pre class="c14"><xsl:value-of select="//xs:element[@name='mime-entry']/xs:annotation/xs:documentation"/></pre>
																<span>Attributes:</span>
																<table>
																	<tbody>
																		<tr class="c16">
																			<td class="c13">
																				<span class="c12">type</span>
																			</td>
																			<td class="c15">
																				<pre class="c14"><xsl:value-of select="//xs:element[@name='mime-entry']//xs:attribute[@name='type']/xs:annotation/xs:documentation"/></pre>
																			</td>
																		</tr>
																		<tr class="c16">
																			<td class="c17">
																				<span class="c12">extensions</span>
																			</td>
																			<td class="c18">
																				<pre class="c14"><xsl:value-of select="//xs:element[@name='mime-entry']//xs:attribute[@name='extensions']/xs:annotation/xs:documentation"/></pre>
																			</td>
																		</tr>
																	</tbody>
																</table>
															</td>
														</tr>
													</table>
												</div>
											</td>
										</tr>
									</tbody>
								</table>
							</td>
						</tr>
					</table>
				</div>
			</td>
		</tr>
	</tbody>
</table>
</xsl:template>
</xsl:stylesheet>